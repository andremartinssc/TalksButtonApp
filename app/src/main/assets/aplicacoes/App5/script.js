let pontuacao = 0;
let tempo = 30;
let intervaloJogo;
let intervaloMarmota;
let posicaoAtual = null;

const pontuacaoElemento = document.getElementById("pontuacao");
const tempoElemento = document.getElementById("tempo");
const posicoes = [
    document.getElementById("posicao1"),
    document.getElementById("posicao2"),
    document.getElementById("posicao3"),
    document.getElementById("posicao4")
];

const imagemMarmota = document.createElement("img");
imagemMarmota.src = "img/marmota.png";
imagemMarmota.classList.add("marmota");

imagemMarmota.addEventListener("click", () => {
    if (posicaoAtual !== null) {
        pontuacao++;
        pontuacaoElemento.textContent = pontuacao;
        removerMarmota();
    }
});

function iniciarJogo() {
    pontuacao = 0;
    tempo = 30;
    pontuacaoElemento.textContent = pontuacao;
    tempoElemento.textContent = tempo;

    removerMarmota();

    intervaloJogo = setInterval(() => {
        tempo--;
        tempoElemento.textContent = tempo;
        if (tempo <= 0) {
            encerrarJogo();
        }
    }, 1000);

    intervaloMarmota = setInterval(() => {
        mostrarMarmota();
    }, 2000);
}

function encerrarJogo() {
    clearInterval(intervaloJogo);
    clearInterval(intervaloMarmota);
    removerMarmota();
    alert("Fim do jogo! Sua pontuação foi: " + pontuacao);
}

function mostrarMarmota() {
    removerMarmota();
    const indice = Math.floor(Math.random() * 4);
    posicaoAtual = indice;
    const posicaoSelecionada = posicoes[indice];

    posicaoSelecionada.appendChild(imagemMarmota);
    posicaoSelecionada.classList.add("glow"); // adiciona o efeito glow

    setTimeout(() => {
        if (posicaoAtual === indice) {
            removerMarmota();
        }
    }, 1000);
}

function removerMarmota() {
    if (posicaoAtual !== null) {
        const posicaoAnterior = posicoes[posicaoAtual];
        if (posicaoAnterior.contains(imagemMarmota)) {
            posicaoAnterior.removeChild(imagemMarmota);
        }
        posicaoAnterior.classList.remove("glow"); // remove o efeito glow
        posicaoAtual = null;
    }
}

// Suporte a teclado
document.addEventListener("keydown", (evento) => {
    const tecla = evento.key;
    if (["1", "2", "3", "4"].includes(tecla)) {
        const indice = parseInt(tecla) - 1;
        if (indice === posicaoAtual) {
            pontuacao++;
            pontuacaoElemento.textContent = pontuacao;
            removerMarmota();
        }
    }
});
