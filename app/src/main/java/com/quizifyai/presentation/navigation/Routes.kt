package com.quizifyai.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val QUIZ_DETAIL = "quiz_detail"
    const val QUIZ_DETAIL_ROUTE = "$QUIZ_DETAIL/{quizId}"

    fun quizDetail(quizId: String) = "$QUIZ_DETAIL/$quizId"
}
