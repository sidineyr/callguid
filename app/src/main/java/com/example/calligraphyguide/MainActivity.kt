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
    private var wordDraft = "Olá"
    private var wordInput: EditText? = null
    private var animationSpeed = 1f
    private var numberedStrokes = false
    private val returnToAlphabet = Runnable { completionScheduled = false; showAlphabet() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ads = AdsController(this)
        upper = state?.getBoolean("upper") ?: false
        autoReturn = state?.getBoolean("autoReturn") ?: true
        selected = state?.getString("letter") ?: "a"
        wordDraft = state?.getString("wordDraft") ?: "Olá"
        animationSpeed = state?.getFloat("speed") ?: 1f
        numberedStrokes = state?.getBoolean("numbers") ?: false
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (page == "welcome" || page == "age") finish()
                else if ((page == "animation" || page == "practice") && selected.length>1) showWords()
                else showAlphabet()
            }
        })
        // Never restore adult eligibility: it is intentionally session-only.
        if (BuildConfig.ADS_ENABLED) showAgeChoice()
        else when (state?.getString("page")) {
            "animation" -> showLetter(selected)
            "alphabet" -> showAlphabet()
            "words" -> showWords()
            "practice" -> showPractice()
            else -> showWelcome()
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun fresh(name: String, scroll: Boolean = true) {
        wordInput?.let { wordDraft = it.text.toString() }; wordInput = null
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
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
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
        button("Escrever uma palavra ou meu nome") { showWords() }
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
        text(if (letter.length==1) "O caminho da letra $letter" else "Sua palavra em movimento", 26f, true)
        if (letter.length>1) text(letter, 22f, true)
        text("↓ Descida: mais pressão · ↑ Subida: traço leve. Observe o alívio da pressão nas curvas e nas saídas.", 15f)
        val view = LetterAnimationView(this, letter)
        view.speed = animationSpeed; view.showNumbers = numberedStrokes
        animation = view
        root.addView(view, LinearLayout.LayoutParams(-1, dp(300)))
        val status = text("Escrevendo…", 16f)
        root.addView(Spinner(this).apply {
            contentDescription = "Velocidade da animação"
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Velocidade lenta · 0,5×", "Velocidade normal · 1×", "Velocidade rápida · 1,5×"))
            setSelection(when(animationSpeed) { .5f -> 0; 1.5f -> 2; else -> 1 })
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    animationSpeed = listOf(.5f,1f,1.5f)[position]; view.speed = animationSpeed
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(Switch(this).apply {
            text="Mostrar a ordem dos traços"; minHeight=dp(48); isChecked=numberedStrokes
            setOnCheckedChangeListener { _,checked -> numberedStrokes=checked; view.showNumbers=checked }
        })
        val pause = button("Pausar animação") { }
        pause.setOnClickListener {
            handler.removeCallbacks(returnToAlphabet); completionScheduled = false
            view.togglePause(); pause.text = if (view.paused) "Continuar animação" else "Pausar animação"
        }
        button(if(letter.length==1) "Repetir a letra" else "Repetir palavra") {
            handler.removeCallbacks(returnToAlphabet); completionScheduled = false
            status.text = "Escrevendo…"; pause.text = "Pausar animação"; view.start()
        }
        button("Praticar este modelo") { showPractice() }
        if (letter.length>1) button("Editar palavra") { wordDraft=letter; showWords() }
        button("Escolher outra letra") { showAlphabet() }
        view.onFinished = {
            status.text = "Traçado concluído. Você também pode praticar no papel."
            status.announceForAccessibility("Escrita de $letter concluída")
            policy.completedLesson()
            val touchExploration = getSystemService(android.view.accessibility.AccessibilityManager::class.java).isTouchExplorationEnabled
            if (autoReturn && letter.length==1 && !touchExploration) {
                completionScheduled = true; handler.postDelayed(returnToAlphabet, 1300L)
            }
        }
        view.post { if (animation === view) view.start() }
    }
    private fun showPractice() {
        fresh("practice")
        text("Seu próprio traço", 28f, true)
        text("$selected · Siga o modelo claro. Não há correção automática nem nota.", 15f)
        val trace = TraceView(this).apply { setTemplate(selected) }
        trace.showNumbers=numberedStrokes
        val sheet=HorizontalScrollView(this).apply {
            isFillViewport=true
            addView(trace, android.view.ViewGroup.LayoutParams(-1,-1))
        }
        root.addView(sheet, LinearLayout.LayoutParams(-1,dp(300)))
        root.addView(Switch(this).apply {
            text="Mostrar modelo"; isChecked=true; minHeight=dp(48)
            setOnCheckedChangeListener { _,checked -> trace.modelVisible=checked }
        })
        val tools=LinearLayout(this); root.addView(tools)
        fun tool(label:String, action:()->Unit) = Button(this).apply {
            text=label; isAllCaps=false; minHeight=dp(48)
            tools.addView(this, LinearLayout.LayoutParams(0,-2,1f)); setOnClickListener { action() }
        }
        val eraser=tool("Borracha") { }
        eraser.setOnClickListener { trace.erasing=!trace.erasing; eraser.text=if(trace.erasing) "Usar caneta" else "Borracha" }
        tool("Desfazer") { trace.undo() }; tool("Limpar") { trace.clear() }
        val colors=LinearLayout(this); root.addView(colors)
        listOf("Verde" to ink,"Azul" to Color.rgb(36,82,150),"Vinho" to Color.rgb(140,48,76)).forEach { (name,color) ->
            colors.addView(Button(this).apply {
                text=name; isAllCaps=false; setTextColor(color); minHeight=dp(48)
                setOnClickListener { trace.penColor=color; trace.erasing=false; eraser.text="Borracha" }
            }, LinearLayout.LayoutParams(0,-2,1f))
        }
        text("Espessura da caneta",13f)
        root.addView(SeekBar(this).apply {
            max=8; progress=2; contentDescription="Espessura da caneta"
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) { trace.penWidthDp=2f+value }
                override fun onStartTrackingTouch(bar:SeekBar?)=Unit
                override fun onStopTrackingTouch(bar:SeekBar?)=Unit
            })
        },LinearLayout.LayoutParams(-1,dp(40)))
        if(selected.length>5) {
            text("Ampliar palavra",13f)
            root.addView(SeekBar(this).apply {
                max=200; contentDescription="Ampliação do papel"
                setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) {
                        trace.layoutParams=trace.layoutParams.apply { width=((sheet.width)*(1f+value/100f)).toInt().coerceAtLeast(1) }
                    }
                    override fun onStartTrackingTouch(bar:SeekBar?)=Unit
                    override fun onStopTrackingTouch(bar:SeekBar?)=Unit
                })
            },LinearLayout.LayoutParams(-1,dp(40)))
            text("Mover papel para os lados",13f)
            root.addView(SeekBar(this).apply {
                max=100; contentDescription="Posição horizontal do papel ampliado"
                setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean) {
                        sheet.scrollTo(((trace.width-sheet.width).coerceAtLeast(0)*value/100f).toInt(),0)
                    }
                    override fun onStartTrackingTouch(bar:SeekBar?)=Unit
                    override fun onStopTrackingTouch(bar:SeekBar?)=Unit
                })
            },LinearLayout.LayoutParams(-1,dp(40)))
        }
        button("Ver animação") { showLetter(selected) }
        button(if(selected.length>1) "Voltar às palavras" else "Voltar ao alfabeto") {
            if(selected.length>1) showWords() else showAlphabet()
        }
    }

    private fun showWords() {
        fresh("words")
        text("Palavras com o seu traço",30f,true)
        text("Digite seu nome, uma palavra ou uma frase curta. Observe a escrita e depois pratique sobre o modelo.",16f)
        val input=EditText(this).apply {
            hint="Ex.: João, coração, minha escola"; setText(wordDraft); textSize=22f
            minHeight=dp(56); setTextColor(ink); maxLines=2
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters=arrayOf(android.text.InputFilter.LengthFilter(64))
            contentDescription="Palavra ou frase para escrever, até 32 caracteres"
        }
        root.addView(input,LinearLayout.LayoutParams(-1,-2)); wordInput=input
        text("Até 32 caracteres. Letras, acentos, ç, números, espaços e os sinais . , - '.",14f)
        fun validated():String? = try {
            val drawing=StrokeRepository.drawing(this,input.text.toString())
            input.error=null; wordDraft=drawing.text; drawing.text
        } catch(e:IllegalArgumentException) { input.error=e.message; input.requestFocus(); null }
        fun hideKeyboard() {
            getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(input.windowToken,0); input.clearFocus()
        }
        button("Observar palavra animada") { validated()?.let { hideKeyboard(); showLetter(it) } }
        button("Desenhar sobre a palavra") { validated()?.let { selected=it; hideKeyboard(); showPractice() } }
        val prefs=getSharedPreferences("words",MODE_PRIVATE)
        fun favorites():MutableList<String> {
            val array=try { org.json.JSONArray(prefs.getString("favorites","[]")) } catch(_:Exception) { org.json.JSONArray() }
            return (0 until array.length()).map { array.getString(it) }.toMutableList()
        }
        button("Salvar palavra neste aparelho") {
            validated()?.let { value ->
                val items=favorites(); items.remove(value); items.add(0,value)
                prefs.edit().putString("favorites",org.json.JSONArray(items.take(12)).toString()).apply()
                hideKeyboard(); showWords()
            }
        }
        text("Sugestões para começar",20f,true)
        listOf("Olá","João","coração","minha escola").forEach { value ->
            button(value) { input.setText(value); input.setSelection(value.length) }
        }
        val saved=favorites()
        if(saved.isNotEmpty()) {
            text("Suas palavras salvas",20f,true)
            saved.forEach { value -> button(value) { input.setText(value); input.setSelection(value.length) } }
            button("Excluir palavras salvas") {
                androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Excluir qual palavra?")
                    .setItems(saved.toTypedArray()) { _,index ->
                        saved.removeAt(index); prefs.edit().putString("favorites",org.json.JSONArray(saved).toString()).apply(); showWords()
                    }.setNegativeButton("Cancelar",null).show()
            }
        }
        text("Palavras salvas ficam somente neste aparelho. Os desenhos não são salvos.",14f)
        button("Voltar ao alfabeto") { hideKeyboard(); showAlphabet() }
    }
    private fun showInfo() {
        fresh("info")
        text("Escrever com cuidado", 28f, true)
        text("Apoie os pés e o antebraço, mantenha os ombros relaxados e evite apertar a caneta. Faça pausas. As letras são modelos para explorar, e não um padrão obrigatório.")
        text("Privacidade", 24f, true)
        text("As letras, palavras e desenhos funcionam no aparelho. O app não pede conta, não envia os traçados e não salva o papel digital. Se você tocar em Salvar palavra, ela fica somente neste aparelho e pode ser excluída na área de palavras. A faixa etária, quando perguntada, só permanece na memória desta sessão.")
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
        outState.putString("wordDraft",wordInput?.text?.toString() ?: wordDraft)
        outState.putFloat("speed",animationSpeed); outState.putBoolean("numbers",numberedStrokes)
        outState.putString("page", page); outState.putString("letter", selected)
        outState.putBoolean("upper", upper); outState.putBoolean("autoReturn", autoReturn)
        super.onSaveInstanceState(outState)
    }
}
