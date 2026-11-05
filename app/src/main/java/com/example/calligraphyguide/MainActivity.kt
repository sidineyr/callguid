package com.sidineyr.callguide

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val ink = Color.rgb(27, 73, 61)
    private val paper = Color.rgb(250, 247, 237)
    private lateinit var root: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val policy = AdBreakPolicy(SystemClock.elapsedRealtime())
    private lateinit var ads: AdsController
    private var animation: LetterAnimationView? = null
    private var page = "welcome"
    private var upper = false
    private var autoReturn = true
    private var selected = "a"
    private var completionScheduled = false
    private var resumeAnimation = false
    private var resumeReturn = false
    private val returnToAlphabet = Runnable { completionScheduled = false; showAlphabet() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ads = AdsController(this)
        upper = state?.getBoolean("upper") ?: false
        autoReturn = state?.getBoolean("autoReturn") ?: true
        selected = state?.getString("letter") ?: "a"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (page == "welcome" || page == "age") finish() else showAlphabet()
            }
        })
        // Never restore adult eligibility: it is intentionally session-only.
        if (BuildConfig.ADS_ENABLED) showAgeChoice()
        else when (state?.getString("page")) {
            "animation" -> showLetter(selected)
            "alphabet" -> showAlphabet()
            else -> showWelcome()
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun fresh(name: String, scroll: Boolean = true) {
        handler.removeCallbacks(returnToAlphabet); completionScheduled = false
        animation?.stop(); animation = null; ads.destroyView()
        page = name
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(paper)
            setPadding(dp(22), dp(12), dp(22), dp(18))
        }
        val host: View = if (scroll) ScrollView(this).apply { isFillViewport = true; addView(root) } else root
        setContentView(host)
        ViewCompat.setOnApplyWindowInsetsListener(host) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            if (view === root) view.setPadding(dp(22) + bars.left, dp(12) + bars.top, dp(22) + bars.right, dp(18) + bars.bottom)
            else view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(host)
    }
    private fun text(value: String, size: Float = 18f, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(ink)
        setPadding(0, dp(8), 0, dp(8))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        root.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; textSize = 17f; minHeight = dp(52)
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(ink)
        root.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        setOnClickListener { action() }
    }
    private fun showAgeChoice() {
        fresh("age")
        text("Antes de começar", 30f, true)
        text("Qual é sua faixa de idade? A escolha vale apenas nesta sessão. Todas as letras estão disponíveis para todos.")
        listOf("Até 12 anos", "13 a 17 anos", "18 anos ou mais", "Prefiro não informar").forEachIndexed { index, label ->
            button(label) { ads.setAdult(index == 2); showWelcome() }
        }
        text("Pessoas menores de 18 anos e quem não informar a idade usam esta versão sem anúncios.", 15f)
    }
    private fun showWelcome() {
        fresh("welcome")
        text("CALLGUIDE  /  ATELIÊ DE LETRAS", 13f, true)
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.welcome_prince)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Pequeno príncipe indígena imaginário com caneta e caderno; ilustração original criada com IA."
        }, LinearLayout.LayoutParams(-1, dp(360)))
        text("Cada letra, um gesto bonito.", 32f, true)
        text("Venha aprender a escrever com calma. Observe a caneta, descubra os caminhos da letra cursiva e encontre o seu próprio traço.")
        button("Vamos escrever") { showAlphabet() }
        text("Gratuito • Letras disponíveis offline • Sem conta", 14f)
    }
    private fun showAlphabet() {
        fresh("alphabet")
        text("Escolha uma letra", 30f, true)
        text("Toque no teclado e acompanhe o movimento da caneta. Cada demonstração dura cerca de quatro segundos.", 16f)
        button(if (upper) "Maiúsculas · mudar para minúsculas" else "Minúsculas · mudar para maiúsculas") {
            upper = !upper; showAlphabet()
        }
        val columns = ((resources.configuration.screenWidthDp - 44) / 56).coerceIn(1, 6)
        val grid = GridLayout(this).apply { columnCount = columns; useDefaultMargins = true }
        root.addView(grid, LinearLayout.LayoutParams(-1, -2))
        val alphabet = if (upper) 'A'..'Z' else 'a'..'z'
        alphabet.forEach { letter ->
            grid.addView(Button(this).apply {
                text = letter.toString(); isAllCaps = false; textSize = 25f
                setTextColor(ink); minWidth = 0; minimumWidth = 0; minHeight = dp(54)
                setPadding(0, 0, 0, 0)
                contentDescription = "Ver traçado da letra $letter " + if (upper) "maiúscula" else "minúscula"
                background = GradientDrawable().apply { setColor(Color.rgb(235, 239, 222)); cornerRadius = dp(12).toFloat() }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; height = dp(54); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
                setOnClickListener { chooseLetter(letter.toString()) }
            })
        }
        root.addView(Switch(this).apply {
            text = "Voltar automaticamente ao alfabeto"; textSize = 16f; setTextColor(ink)
            isChecked = autoReturn; minHeight = dp(52)
            setOnCheckedChangeListener { _, checked -> autoReturn = checked }
        })
        button("Praticar no papel digital") { showPractice() }
        button("Orientações e privacidade") { showInfo() }
    }
    private fun chooseLetter(letter: String) {
        selected = letter
        val now = SystemClock.elapsedRealtime()
        if (ads.canShow() && policy.shouldOffer(now)) {
            policy.offered(now); fresh("break")
            text("Uma pausa entre as letras", 28f, true)
            text("Publicidade", 14f, true)
            ads.attach(root)
            text("Você pode continuar agora. Não é necessário tocar no anúncio.", 16f)
            button("Continuar para a letra $letter") { showLetter(letter) }
        } else showLetter(letter)
    }
    private fun showLetter(letter: String) {
        fresh("animation"); selected = letter
        text("O caminho da letra $letter", 26f, true)
        text("↓ Descida: mais pressão · ↑ Subida: traço leve. Observe o alívio da pressão nas curvas e nas saídas.", 15f)
        val view = LetterAnimationView(this, letter)
        animation = view
        root.addView(view, LinearLayout.LayoutParams(-1, dp(300)))
        val status = text("Escrevendo…", 16f)
        val pause = button("Pausar animação") { }
        pause.setOnClickListener {
            handler.removeCallbacks(returnToAlphabet); completionScheduled = false
            view.togglePause(); pause.text = if (view.paused) "Continuar animação" else "Pausar animação"
        }
        button("Repetir a letra") {
            handler.removeCallbacks(returnToAlphabet); completionScheduled = false
            status.text = "Escrevendo…"; pause.text = "Pausar animação"; view.start()
        }
        button("Escolher outra letra") { showAlphabet() }
        view.onFinished = {
            status.text = "Traçado concluído. Você também pode praticar no papel."
            status.announceForAccessibility("Traçado da letra $letter concluído")
            policy.completedLesson()
            val touchExploration = getSystemService(android.view.accessibility.AccessibilityManager::class.java).isTouchExplorationEnabled
            if (autoReturn && !touchExploration) {
                completionScheduled = true; handler.postDelayed(returnToAlphabet, 1300L)
            }
        }
        view.post { if (animation === view) view.start() }
    }
    private fun showPractice() {
        fresh("practice", false)
        text("Seu próprio traço", 28f, true)
        text("Pratique a letra $selected. Não há nota: procure conforto e legibilidade.", 16f)
        val trace = TraceView(this)
        root.addView(trace, LinearLayout.LayoutParams(-1, 0, 1f))
        button("Desfazer último traço") { trace.undo() }
        button("Limpar papel") { trace.clear() }
        button("Voltar ao alfabeto") { showAlphabet() }
    }
    private fun showInfo() {
        fresh("info")
        text("Escrever com cuidado", 28f, true)
        text("Apoie os pés e o antebraço, mantenha os ombros relaxados e evite apertar a caneta. Faça pausas. As letras são modelos para explorar, e não um padrão obrigatório.")
        text("Privacidade", 24f, true)
        text("As letras e os desenhos funcionam no aparelho. O app não pede conta, não envia os traçados e não salva o papel digital. A faixa etária, quando perguntada, só permanece na memória desta sessão.")
        text(if (BuildConfig.ADS_ENABLED) "Publicidade entre lições para maiores de 18 anos. A rede Google AdMob pode processar IP, dados técnicos e interações. O app não solicita anúncios para menores ou idade não informada. Em versões debug, todos os anúncios são de teste." else "Esta versão gratuita não contém SDK de publicidade nem solicita anúncios.")
        text("Contato: sidiney@edu.univali.br", 16f)
        button("Ler política de privacidade completa") {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
        }
        button("Opções de privacidade dos anúncios") { ads.privacyOptions() }
        button("Voltar ao alfabeto") { showAlphabet() }
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (page == "alphabet" && event.action == KeyEvent.ACTION_UP) {
            val ch = event.unicodeChar.toChar()
            if (ch in 'a'..'z' || ch in 'A'..'Z') {
                chooseLetter(if (upper) ch.uppercase() else ch.lowercase()); return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onPause() {
        resumeAnimation = animation?.isPlaying == true
        resumeReturn = completionScheduled
        animation?.pause(); handler.removeCallbacks(returnToAlphabet); completionScheduled = false
        ads.pause(); super.onPause()
    }
    override fun onResume() {
        super.onResume()
        if (::ads.isInitialized) ads.resume()
        if (resumeAnimation && animation?.paused == true) animation?.togglePause()
        if (resumeReturn && page == "animation") {
            completionScheduled = true; handler.postDelayed(returnToAlphabet, 1300L)
        }
        resumeAnimation = false; resumeReturn = false
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); animation?.stop(); ads.destroyView(); super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", page); outState.putString("letter", selected)
        outState.putBoolean("upper", upper); outState.putBoolean("autoReturn", autoReturn)
        super.onSaveInstanceState(outState)
    }
}
