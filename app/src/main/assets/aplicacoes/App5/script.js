let pontuacao = 0;
let tempo = 30;
let intervaloJogo;
let intervaloMarmota;
let posicaoAtual = null; // Armazena o índice da posição onde a marmota está (0 a 3)

const pontuacaoElemento = document.getElementById("pontuacao");
const tempoElemento = document.getElementById("tempo");
const posicoes = [
    document.getElementById("button1"),
    document.getElementById("button2"),
    document.getElementById("button3"),
    document.getElementById("button4")
];

const imagemMarmota = document.createElement("img");
imagemMarmota.src = "img/marmota.png";
imagemMarmota.classList.add("marmota");

let audioJogo; // Variável global para o áudio de fundo
// REMOVA ESTA LINHA: let audioSoco; // Variável global para o áudio do soco

// Evento de clique na marmota (para toque na tela ou mouse)
imagemMarmota.addEventListener("click", () => {
    if (posicaoAtual !== null) {
        pontuacao++;
        pontuacaoElemento.textContent = pontuacao;

        // --- MUDANÇA: REPRODUZIR ÁUDIO DE SOCO CRIANDO UMA NOVA INSTÂNCIA ---
        const socoSound = new Audio('audio/soco.mp3'); // Cria um novo objeto Audio
        socoSound.volume = 0.7; // Define o volume (você pode ajustar)
        socoSound.play().catch(error => {
            console.warn("Problema ao tentar reproduzir o áudio de soco (clique):", error);
        });
        // --- FIM MUDANÇA ---

        removerMarmota();
        console.log(`Marmota acertada por CLIQUE na posição ${posicaoAtual + 1}.`);
    }
});

function iniciarJogo() {
    clearInterval(intervaloJogo);
    clearInterval(intervaloMarmota);

    pontuacao = 0;
    tempo = 30;
    pontuacaoElemento.textContent = pontuacao;
    tempoElemento.textContent = tempo;

    removerMarmota();

    if (audioJogo) {
        audioJogo.currentTime = 0;
        audioJogo.play().catch(error => {
            console.warn("Problema ao tentar reproduzir o áudio. Pode ser necessário uma interação do usuário.", error);
        });
    }

    intervaloJogo = setInterval(() => {
        tempo--;
        tempoElemento.textContent = tempo;
        if (tempo <= 0) {
            encerrarJogo();
        }
    }, 1000);

    intervaloMarmota = setInterval(() => {
        mostrarMarmota();
    }, 1500);
}

function encerrarJogo() {
    clearInterval(intervaloJogo);
    clearInterval(intervaloMarmota);
    removerMarmota();

    for (let i = 1; i <= 4; i++) {
        window.location.href = `talksbutton://led/${i}/0`;
    }
    console.log("Fim do jogo! Desligando todos os LEDs.");

    if (audioJogo) {
        audioJogo.pause();
        audioJogo.currentTime = 0;
    }

    alert("Fim do jogo! Sua pontuação foi: " + pontuacao);
}

function mostrarMarmota() {
    removerMarmota();

    const indiceAleatorio = Math.floor(Math.random() * 4);
    posicaoAtual = indiceAleatorio;
    const posicaoSelecionada = posicoes[indiceAleatorio];

    posicaoSelecionada.appendChild(imagemMarmota);
    posicaoSelecionada.classList.add("glow");

    window.location.href = `talksbutton://led/${posicaoAtual + 1}/1000`;
    console.log(`Marmota apareceu na posição ${posicaoAtual + 1}. Acendendo LED ${posicaoAtual + 1}.`);

    setTimeout(() => {
        if (posicaoAtual === indiceAleatorio) {
            removerMarmota();
            console.log(`Marmota na posição ${posicaoAtual + 1} escapou!`);
        }
    }, 1000);
}

function removerMarmota() {
    if (posicaoAtual !== null) {
        const posicaoAnterior = posicoes[posicaoAtual];
        if (posicaoAnterior.contains(imagemMarmota)) {
            posicaoAnterior.removeChild(imagemMarmota);
        }
        posicaoAnterior.classList.remove("glow");

        window.location.href = `talksbutton://led/${posicaoAtual + 1}/0`;
        console.log(`Marmota removida da posição ${posicaoAtual + 1}. Desligando LED ${posicaoAtual + 1}.`);

        posicaoAtual = null;
    }
}

// Suporte a teclado
document.addEventListener("keydown", (evento) => {
    const tecla = evento.key;

    console.log(`[JOGO MARMOTA DEBUG] Tecla recebida: '${tecla}'`);
    console.log(`[JOGO MARMOTA DEBUG] Estado atual: posicaoAtual = ${posicaoAtual}`);

    let indiceDaTecla = -1;

    switch (tecla) {
        case '1':
            indiceDaTecla = 0;
            break;
        case '2':
        case '0':
            indiceDaTecla = 1;
            break;
        case '3':
        case 'x':
            indiceDaTecla = 2;
            break;
        case '4':
        case 'm':
            indiceDaTecla = 3;
            break;
        default:
            console.log(`[JOGO MARMOTA DEBUG] Tecla '${tecla}' não é um comando de botão válido para o jogo.`);
            return;
    }

    console.log(`[JOGO MARMOTA DEBUG] Botão Bluetooth ${indiceDaTecla + 1} Pressionado.`);

    if (indiceDaTecla === posicaoAtual) {
        pontuacao++;
        pontuacaoElemento.textContent = pontuacao;

        // --- MUDANÇA: REPRODUZIR ÁUDIO DE SOCO CRIANDO UMA NOVA INSTÂNCIA ---
        const socoSound = new Audio('audio/soco.mp3'); // Cria um novo objeto Audio
        socoSound.volume = 0.7; // Define o volume (você pode ajustar)
        socoSound.play().catch(error => {
            console.warn("Problema ao tentar reproduzir o áudio de soco (keydown):", error);
        });
        // --- FIM MUDANÇA ---

        removerMarmota();
        console.log(`[JOGO MARMOTA DEBUG] Marmota ACERTADA via Bluetooth/Teclado na posição ${indiceDaTecla + 1}. Pontuação: ${pontuacao}`);
    } else {
        console.log(`[JOGO MARMOTA DEBUG] Erro! Pressionado botão ${indiceDaTecla + 1}, mas marmota está na posição ${posicaoAtual !== null ? posicaoAtual + 1 : 'nenhuma'}.`);
    }
});

document.addEventListener('DOMContentLoaded', () => {
    audioJogo = document.getElementById('audioJogo');
    if (audioJogo) {
        audioJogo.volume = 0.3;
    }

    // REMOVA ESTAS LINHAS:
    // audioSoco = document.getElementById('audioSoco');
    // if (audioSoco) {
    //     audioSoco.volume = 0.7;
    // }

    console.log("Jogo da Marmota carregado. Pronto para iniciar!");
});