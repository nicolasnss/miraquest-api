-- Seed idempotente: só insere se as tabelas estiverem vazias.
INSERT INTO usuario_progresso (email_usuario, vidas, xp_total, streak_dias)
SELECT 'teste@miraquest.com', 5, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM usuario_progresso);

INSERT INTO questao (materia, tipo, enunciado, resposta_correta, opcoes_distracao, explicacao)
SELECT v.* FROM (VALUES
('Redes', 'CLOZE', 'No modelo OSI, a camada responsável por roteamento entre redes é a camada de ___.', 'Rede', 'Transporte|Física|Aplicação', 'A camada de Rede (camada 3) trata endereçamento lógico e encaminhamento de pacotes entre redes distintas.'),
('Redes', 'CLOZE', 'O protocolo ___ é sem conexão e não garante entrega; amplamente usado para DNS e streaming em tempo real.', 'UDP', 'TCP|HTTP|FTP', 'UDP não estabelece sessão antes do envio e não retransmite pacotes perdidos, o que reduz latência.'),
('Redes', 'CLOZE', 'Um endereço IPv4 possui ___ bits de comprimento típico expresso em quatro octetos decimais.', '32', '64|128|48', 'IPv4 usa 32 bits (4 × 8 bits), permitindo cerca de 4,3 bilhões de endereços teóricos.'),
('Linux', 'CERTO_ERRADO', 'No Linux, o comando `ls -la` lista arquivos incluindo ocultos, com detalhes em formato longo.', 'CERTO', NULL, '`ls -a` inclui entradas que começam com ponto; `-l` usa listagem longa com permissões e donos.'),
('Linux', 'CERTO_ERRADO', 'O diretório `/etc` no FHS guarda principalmente arquivos de configuração do sistema.', 'CERTO', NULL, 'O Filesystem Hierarchy Standard define `/etc` para configurações locais do host, não para binários.'),
('Linux', 'CERTO_ERRADO', '`systemctl restart nginx` apenas recarrega a configuração do Nginx sem interromper o master process.', 'ERRADO', NULL, '`restart` para e inicia a unidade; recarga leve costuma ser `reload` quando a unidade suporta sinal de recarga.')
) AS v(materia, tipo, enunciado, resposta_correta, opcoes_distracao, explicacao)
WHERE NOT EXISTS (SELECT 1 FROM questao);
