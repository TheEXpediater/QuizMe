"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const {defineSecret} = require("firebase-functions/params");
const {onObjectFinalized} = require("firebase-functions/v2/storage");
const fs = require("fs/promises");
const os = require("os");
const path = require("path");
const {PDFParse} = require("pdf-parse");

admin.initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = "gemini-2.5-flash";
const MAX_TEXT_CHARS = 18000;

exports.processUploadedPdf = onObjectFinalized(
  {
    region: "us-central1",
    memory: "512MiB",
    timeoutSeconds: 120,
    maxInstances: 2,
    secrets: [geminiApiKey],
  },
  async (event) => {
    const object = event.data;
    const filePath = object.name || "";
    const contentType = object.contentType || "";
    const metadata = object.metadata || {};

    if (!filePath.startsWith("pdfs/")) return;
    if (metadata.generateQuiz === "false") {
      logger.info("Skipping server quiz generation by metadata flag.", {filePath});
      return;
    }
    if (contentType && contentType !== "application/pdf") {
      logger.warn("Skipping non-PDF upload.", {filePath, contentType});
      return;
    }

    const segments = filePath.split("/");
    const userId = segments[1];
    if (!userId) {
      logger.warn("Skipping PDF without user id path.", {filePath});
      return;
    }

    const bucket = admin.storage().bucket(object.bucket);
    const tempFile = path.join(os.tmpdir(), path.basename(filePath));

    try {
      await bucket.file(filePath).download({destination: tempFile});
      const text = await extractPdfText(tempFile);
      const quiz = await generateQuiz(text, path.basename(filePath));
      await saveQuiz(userId, quiz);
      logger.info("Generated quiz from PDF.", {userId, filePath});
    } finally {
      await fs.unlink(tempFile).catch(() => undefined);
    }
  },
);

async function extractPdfText(tempFile) {
  const buffer = await fs.readFile(tempFile);
  const parser = new PDFParse({data: buffer});

  try {
    const parsed = await parser.getText();
    const text = (parsed.text || "").replace(/\s+/g, " ").trim();

    if (!text) {
      throw new Error("No readable text was found in the PDF.");
    }

    return text.slice(0, MAX_TEXT_CHARS);
  } finally {
    if (typeof parser.destroy === "function") {
      await parser.destroy();
    }
  }
}

async function generateQuiz(text, pdfName) {
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": geminiApiKey.value(),
      },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              {
                text: buildPrompt(text),
              },
            ],
          },
        ],
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 4096,
          responseMimeType: "application/json",
          responseSchema: quizSchema(),
        },
      }),
    },
  );

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Gemini request failed: ${response.status} ${errorText}`);
  }

  const data = await response.json();
  const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "";
  const payload = JSON.parse(extractJsonObject(rawText));
  const questions = (payload.questions || [])
    .map(normalizeQuestion)
    .filter(Boolean)
    .slice(0, 10);

  if (!questions.length) {
    throw new Error("Gemini returned no valid questions.");
  }

  return {
    title: String(payload.title || pdfName.replace(/\.pdf$/i, "")).trim().slice(0, 80),
    questions,
    pdfName,
  };
}

function buildPrompt(text) {
  return [
    "Generate exactly 10 multiple choice questions from this academic content.",
    "Include a concise title for the quiz.",
    "Each question must include question, 4 choices, correctAnswer, and explanation.",
    "Return JSON only. Do not include markdown.",
    "",
    "Academic content:",
    text,
  ].join("\n");
}

function quizSchema() {
  const questionSchema = {
    type: "OBJECT",
    properties: {
      question: {type: "STRING"},
      choices: {
        type: "ARRAY",
        items: {type: "STRING"},
      },
      correctAnswer: {type: "STRING"},
      explanation: {type: "STRING"},
    },
    required: ["question", "choices", "correctAnswer", "explanation"],
  };

  return {
    type: "OBJECT",
    properties: {
      title: {type: "STRING"},
      questions: {
        type: "ARRAY",
        items: questionSchema,
      },
    },
    required: ["title", "questions"],
  };
}

function extractJsonObject(raw) {
  const withoutFence = String(raw)
    .replace(/```json/gi, "")
    .replace(/```/g, "")
    .trim();

  const start = withoutFence.indexOf("{");
  if (start === -1) return withoutFence;

  let depth = 0;
  let inString = false;
  let escaped = false;

  for (let index = start; index < withoutFence.length; index += 1) {
    const char = withoutFence[index];
    if (escaped) {
      escaped = false;
    } else if (char === "\\" && inString) {
      escaped = true;
    } else if (char === "\"") {
      inString = !inString;
    } else if (!inString && char === "{") {
      depth += 1;
    } else if (!inString && char === "}") {
      depth -= 1;
      if (depth === 0) return withoutFence.slice(start, index + 1);
    }
  }

  return withoutFence.slice(start);
}

function normalizeQuestion(item) {
  const choices = Array.from(
    new Set((item.choices || []).map((choice) => String(choice).trim()).filter(Boolean)),
  ).slice(0, 4);

  const question = String(item.question || "").trim();
  if (!question || choices.length !== 4) return null;

  return {
    question,
    choices,
    correctAnswer: normalizeAnswer(item.correctAnswer, choices),
    explanation: String(item.explanation || "Review the source material for this answer.").trim(),
  };
}

function normalizeAnswer(answer, choices) {
  const value = String(answer || "").trim();
  const exact = choices.find((choice) => choice.toLowerCase() === value.toLowerCase());
  if (exact) return exact;

  const letterMap = {A: 0, B: 1, C: 2, D: 3};
  const index = letterMap[value.replace(".", "").toUpperCase()];
  return choices[index] || value || choices[0];
}

async function saveQuiz(userId, quiz) {
  const now = Date.now();
  const quizRef = admin.firestore().collection("quizzes").doc();
  const data = {
    id: quizRef.id,
    userId,
    title: quiz.title,
    questions: quiz.questions,
    createdAt: now,
    pdfName: quiz.pdfName,
  };

  await quizRef.set(data);
  await admin.firestore().collection("history").add({
    userId,
    quizId: quizRef.id,
    pdfName: quiz.pdfName,
    type: "quiz_generated_by_function",
    createdAt: now,
  });
}
