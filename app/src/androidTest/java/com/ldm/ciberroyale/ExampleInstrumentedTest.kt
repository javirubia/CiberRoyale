package com.ldm.ciberroyale

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ldm.ciberroyale", appContext.packageName)
    }

    // Nivel 1 Tests
    @Test
    fun testNivel1_AttackAnimationScreen() {
        onView(withId(R.id.scroll_pantalla_ataque))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnSiguienteAtaque))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_BytePresentationScreen() {
        onView(withId(R.id.scroll_pantalla_byte))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnUnirse))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnRechazar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_ContextScreen() {
        onView(withId(R.id.scroll_pantalla_contexto))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnContextoListo))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_QuizScreen() {
        onView(withId(R.id.scroll_pantalla_quiz))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tvPregunta))
            .check(matches(isDisplayed()))
        onView(withId(R.id.rgOpciones))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_Mejora1Screen() {
        onView(withId(R.id.scroll_pantalla_mejora1))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinner_m1_p1))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinner_m1_p2))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_Mejora2Screen() {
        onView(withId(R.id.scroll_pantalla_mejora2))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinner_m2_p1))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinner_m2_p2))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_CombateScreen() {
        onView(withId(R.id.scroll_pantalla_combate))
            .check(matches(isDisplayed()))
        onView(withId(R.id.inputContrasena))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnAtacar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel1_RecompensaScreen() {
        onView(withId(R.id.scroll_pantalla_recomp))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tvRecompensaTitulo))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnRecompContinuar))
            .check(matches(isDisplayed()))
    }

    // Nivel 2 Tests
    @Test
    fun testNivel2_IntroScreen() {
        onView(withId(R.id.pantallaIntro))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnIntroSiguiente))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel2_DetectScreen() {
        onView(withId(R.id.pantallaDetec))
            .check(matches(isDisplayed()))
        onView(withId(R.id.targetSeguro))
            .check(matches(isDisplayed()))
        onView(withId(R.id.targetPhishing))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel2_UrlScreen() {
        onView(withId(R.id.pantallaUrl))
            .check(matches(isDisplayed()))
        onView(withId(R.id.targetValida))
            .check(matches(isDisplayed()))
        onView(withId(R.id.targetInvalida))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel2_CombateScreen() {
        onView(withId(R.id.pantallaCombate))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnStartBattle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNivel2_RecompensaScreen() {
        onView(withId(R.id.scroll_pantalla_recomp))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tvRecompensaTitulo))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnRecompContinuar))
            .check(matches(isDisplayed()))
    }
}
