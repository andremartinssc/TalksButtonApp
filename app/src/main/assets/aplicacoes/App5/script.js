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
  document.getElementById("posicao4"),
];

// Cria a imagem da marmota
const imagemMarmota = document.createElement("img");
imagemMarmota.src = "img/marmota.png";
imagemMarmota.classList.add("marmota");

function iniciarJogo() {
  pontuacao = 0;
  tempo = 30;
  pontuacaoElemento.textContent = pontuacao;
  tempoElemento.textContent = tempo;

  removerMarmota();

  // Intervalo de 1 segundo para diminuir a velocidade
  intervaloJogo = setInterval(() => {
    tempo--;
    tempoElemento.textContent = tempo;

    if (tempo <= 0) {
      encerrarJogo();
    }
  }, 1000);  // Intervalo de 1 segundo

  // Intervalo de 2 segundos para a marmota aparecer
  intervaloMarmota = setInterval(() => {
    mostrarMarmota();
  }, 2000);  // Intervalo de 2 segundos
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
  posicoes[indice].appendChild(imagemMarmota);

  // Marmota desaparece após 1 segundo (não afetando o intervalo da próxima)
  setTimeout(() => {
    if (posicaoAtual === indice) {
      removerMarmota();
    }
  }, 1000);  // Marmota desaparece após 1 segundo
}

function removerMarmota() {
  if (posicaoAtual !== null) {
    posicoes[posicaoAtual].innerHTML = "";
    posicaoAtual = null;
  }
}

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
