let somAtual = null;
let modoEdicao = false;
let ledPiscaInterval = null; // Variável para armazenar o ID do intervalo do pisca
let currentLedId = -1; // Armazena o ID do LED que está atualmente piscando

// Constante para o prefixo do comando do LED via TalksButton
const LED_COMMAND_PREFIX = 'talksbutton://led/';

// Função para tocar os sons
function tocarSom(id) {
    if (modoEdicao) return; // Ignora o evento se estiver no modo de edição

    const som = document.getElementById(id);
    const container = document.querySelector(`[onclick="tocarSom('${id}')"]`);

    // Adiciona a animação ao container
    container.classList.add('clique-ativo');

    // Remove a animação após a duração
    setTimeout(() => {
        container.classList.remove('clique-ativo');
    }, 400); // Duração da animação

    // Para e desliga qualquer LED que esteja piscando atualmente antes de iniciar um novo som
    // Esta linha é importante para garantir a limpeza antes de qualquer nova ação
    pararPiscaLedAtual();

    // Reprodução do som (lógica mantida inalterada, conforme solicitado)
    if (somAtual === som && !som.paused) {
        // Se o mesmo som está tocando, pausa e reinicia
        som.pause();
        som.currentTime = 0;
        somAtual = null;

        // Ao pausar, garanta que o LED do som pausado seja desligado
        const ledId = getIdFromSoundId(id);
        if (ledId !== -1) {
            enviarComandoLed(ledId, 0); // Desliga o LED explicitamente
        }
    } else {
        // Se um novo som (ou o mesmo som parado) vai tocar
        if (somAtual && !somAtual.paused) {
            // Pausa o som anterior se estiver tocando
            somAtual.pause();
            somAtual.currentTime = 0;
            // Desliga o LED do som anterior, se houver
            const previousLedId = getIdFromSoundId(somAtual.id);
            if (previousLedId !== -1) {
                enviarComandoLed(previousLedId, 0);
            }
        }
        som.play();
        somAtual = som;

        // Inicia o padrão de pisca para o LED correspondente
        const ledId = getIdFromSoundId(id);
        if (ledId !== -1) {
            currentLedId = ledId; // Armazena o ID do LED atual

            // Primeiro acionamento mais longo para indicar o clique
            enviarComandoLed(ledId, 1000); // LED acende por 1000ms

            // Inicia o padrão de piscadas da música APÓS o acionamento longo inicial.
            // O LED estará desligado automaticamente pela LedController após 1000ms (ajustado para ser consistente).
            setTimeout(() => {
                // *** REGRA CRUCIAL AQUI ***
                // Só inicia o pisca se esta ainda for a música atualmente em reprodução.
                // Isso evita que um pisca comece se a música já foi trocada ou pausada rapidamente.
                if (somAtual === som) {
                    iniciarPiscaLed(ledId);
                } else {
                    // Se a música já mudou, garante que o LED seja desligado.
                    enviarComandoLed(ledId, 0);
                }
            }, 1000); // Espera o LED inicial se desligar antes de iniciar o pisca
        }
    }

    // Evento para quando o som termina de tocar naturalmente
    som.onended = () => {
        console.log(`Som ${id} terminou.`);
        somAtual = null;
        pararPiscaLedAtual(); // Garante que o LED pare de piscar e desligue
    };
}

// Mapeia o ID do som para o ID do LED
function getIdFromSoundId(soundId) {
    switch (soundId) {
        case 'som1': return 1;
        case 'som2': return 2;
        case 'som3': return 3;
        case 'som4': return 4;
        default: return -1; // Retorna -1 para IDs de som não mapeados
    }
}

// Função auxiliar para enviar comandos ao aplicativo Android (via WebViewClient)
// Usamos uma duração de 0 para desligar o LED imediatamente.
// Para ligar, a duração será o tempo que ele ficará aceso antes de a LedController desligá-lo.
function enviarComandoLed(ledNum, duracaoMs) {
    if (typeof window.location.href === 'string') { // Verificação mais robusta para ambiente WebView
        const command = `talksbutton://led/${ledNum}/${duracaoMs}`;
        window.location.href = command;
        console.log(`Comando enviado para LED: ${command}`);
    } else {
        console.warn("Ambiente não Android ou WebView não detectado, comando de LED não será enviado.");
    }
}

// Inicia o padrão de pisca (500ms ligado, 500ms desligado)
// Este padrão é para a indicação da música tocando
function iniciarPiscaLed(ledId) {
    // Limpa qualquer intervalo existente antes de iniciar um novo para evitar múltiplos piscas
    clearInterval(ledPiscaInterval);

    // *** REGRA CRUCIAL AQUI: Só inicia o pisca se a música correspondente estiver ativa ***
    // Se a música não está tocando ou não é a que corresponde a este LED, não inicia o pisca.
    if (!somAtual || getIdFromSoundId(somAtual.id) !== ledId || somAtual.paused) {
        enviarComandoLed(ledId, 0); // Garante que o LED esteja desligado
        currentLedId = -1; // Reseta o LED atual
        return; // Não inicia o intervalo de pisca
    }

    // A cada 1 segundo, enviaremos um comando para ligar o LED por 500ms.
    ledPiscaInterval = setInterval(() => {
        // *** REGRA CRUCIAL AQUI: Verifica continuamente se a música ainda está tocando ***
        if (somAtual && getIdFromSoundId(somAtual.id) === ledId && !somAtual.paused) {
            enviarComandoLed(ledId, 500); // Liga o LED por 500ms via LedController no Android
        } else {
            // Se a música parou, mudou ou foi pausada, para o pisca e desliga o LED
            console.log(`Música ${id} não está mais tocando. Parando pisca do LED ${ledId}.`);
            pararPiscaLedAtual();
        }
    }, 1000); // Repete a cada 1 segundo (500ms ligado + 500ms desligado)
}

// Para o pisca do LED e o desliga
function pararPiscaLedAtual() {
    if (ledPiscaInterval) {
        clearInterval(ledPiscaInterval);
        ledPiscaInterval = null;
    }
    // Garante que o último LED que estava ativo seja desligado
    if (currentLedId !== -1) {
        enviarComandoLed(currentLedId, 0); // Desliga o LED imediatamente
        currentLedId = -1; // Reseta o LED atual
    }
}

// Função para alternar entre o modo de edição
function toggleModoEdicao() {
    modoEdicao = !modoEdicao;
    if (modoEdicao) {
        document.body.classList.add('modo-edicao');
        // Ao entrar no modo de edição, para qualquer pisca e desliga o LED
        pararPiscaLedAtual();
        if (somAtual && !somAtual.paused) {
            somAtual.pause();
            somAtual.currentTime = 0;
            somAtual = null;
        }
    } else {
        document.body.classList.remove('modo-edicao');
    }
    console.log(`Modo Edição: ${modoEdicao ? 'ATIVADO' : 'DESATIVADO'}`);
}

// Evento para as teclas
document.addEventListener('keydown', (event) => {
    if (modoEdicao) return; // Ignora o evento se estiver no modo de edição

    switch (event.key) {
        case '1':
            tocarSom('som1');
            break;
        case '2':
        case '0':
            tocarSom('som2');
            break;
        case '3':
        case 'x':
            tocarSom('som3');
            break;
        case '4':
        case 'm':
            tocarSom('som4');
            break;
        default:
            break; // Ignora outras teclas
    }
});

// Listener de keyup (mantido como uma medida de segurança, se necessário)
document.addEventListener('keyup', (event) => {
    // No contexto atual, com a lógica de LED refinada, este `keyup` é menos crítico.
    // Pode ser removido se não houver outra necessidade para ele.
});