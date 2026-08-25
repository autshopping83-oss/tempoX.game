package cloud.bizflow.tempox.data

/**
 * Plain-text legal documents mirrored from the official web pages
 * (tempox.biz-flow.cloud/privacidade and /termos, August 2026).
 * Rendered natively so the game stays 100% offline-compliant.
 */
object LegalTexts {
    const val PRIVACY_UPDATED = "TEMPOX: Brain & Reflex · Pacote cloud.bizflow.tempox · Última atualização: agosto de 2026"

    val PRIVACY_POLICY: String = """
        |1. QUEM É O DESENVOLVEDOR RESPONSÁVEL
        |
        |O aplicativo TEMPOX: Brain & Reflex é desenvolvido e mantido por Elias Chanisso / Biz-Flow. Para qualquer assunto relacionado a privacidade, utilize o canal de contato ao final desta política.
        |
        |2. DADOS QUE O APLICATIVO COLETA
        |
        |O TEMPOX foi projetado para funcionar majoritariamente offline. Quando há tratamento de dados, ele se limita a:
        |
        |• Identificadores de publicidade anônimos: quando anúncios são exibidos via Google AdMob, podem ser usados identificadores publicitários do dispositivo para seleção e frequência de anúncios. Você pode revogar esse identificador nas configurações do seu Android (Privacidade → Anúncios → Excluir identificador de publicidade).
        |• Verificação de compras in-app: compras realizadas (ex.: remoção de anúncios) são processadas pelo Google Play Billing; tokens de compra são validados junto à Google Play Store para garantir a entrega do produto.
        |• Métricas básicas de crash e performance: em caso de falhas, dados técnicos anônimos (tipo de erro, modelo do aparelho, versão do sistema) podem ser coletados exclusivamente para correção e estabilidade.
        |• Dados locais: pontuação, XP, moedas, recordes e modos desbloqueados são armazenados apenas no próprio dispositivo (preferências locais), sem envio a servidores do desenvolvedor.
        |
        |Não coletamos nome, e-mail, telefone, localização precisa nem dados sensíveis.
        |
        |3. SEUS DIREITOS (LGPD / GDPR)
        |
        |Conforme a Lei Geral de Proteção de Dados (LGPD) e o Regulamento Geral de Proteção de Dados da UE (GDPR), você pode solicitar a qualquer momento:
        |
        |• Confirmação de tratamento e acesso aos dados;
        |• Correção de dados incompletos ou desatualizados;
        |• Exclusão dos dados tratados;
        |• Revogação do consentimento (por exemplo, desativando personalização de anúncios no sistema ou removendo o aplicativo, o que elimina os dados locais).
        |
        |Basta enviar uma solicitação ao e-mail de suporte abaixo; responderemos dentro do prazo legal.
        |
        |4. PRIVACIDADE INFANTIL
        |
        |O TEMPOX não direciona marketing a menores e declara conformidade com as diretrizes de proteção à criança (COPPA/GDPR-K e políticas do Google Play Families). Não coletamos intencionalmente dados de crianças menores de 13 anos. Responsáveis que identifiquem qualquer coleta indevida devem contatar o suporte para eliminação imediata.
        |
        |5. PERMISSÕES UTILIZADAS
        |
        |• Vibração: retorno tátil nos acertos e combos do jogo.
        |• Sem acesso a câmera, microfone, contatos, arquivos ou localização.
        |
        |6. ALTERAÇÕES NESTA POLÍTICA
        |
        |Mudanças relevantes serão refletidas nesta página com nova data de atualização. O uso continuado do app após alterações implica ciência da versão vigente.
        |
        |7. CANAIS OFICIAIS DE SUPORTE
        |
        |Pedidos de exclusão de dados, revogação de consentimento ou qualquer dúvida de privacidade podem ser enviados pelos canais oficiais:
        |
        |• E-mail: bizflow.cloud@gmail.com
        |• WhatsApp: +258 840636794
    """.trimMargin()

    const val TERMS_UPDATED = "TEMPOX: Brain & Reflex · Pacote cloud.bizflow.tempox · Última atualização: agosto de 2026"

    val TERMS_AND_CONDITIONS: String = """
        |1. ACEITAÇÃO DOS TERMOS
        |
        |Ao baixar, instalar ou utilizar o TEMPOX: Brain & Reflex, você declara ter lido e concordado com estes Termos de Uso e com a nossa Política de Privacidade. Caso não concorde, não utilize o aplicativo.
        |
        |2. CONDIÇÕES DE USO
        |
        |• O jogo é destinado ao entretenimento pessoal e não comercial;
        |• É vedado engenheiro-reverso, modificar, descompilar ou distribuir versões alteradas do app;
        |• É vedado usar métodos automatizados para manipular pontuações, moedas ou conquistas;
        |• O uso indevido que comprometa a experiência de outros usuários ou os serviços da Google Play poderá resultar em suspensão do acesso aos recursos online.
        |
        |3. PROPRIEDADE INTELECTUAL
        |
        |Todos os direitos sobre o código-fonte, mecânicas de jogo, arte gráfica, sons, identidade visual e a marca TEMPOX pertencem a Elias Chanisso / Biz-Flow, protegidos pela legislação de propriedade intelectual brasileira e internacional. Estes termos concedem apenas uma licença limitada, revogável e não exclusiva para uso pessoal do aplicativo.
        |
        |4. COMPRAS DENTRO DO APLICATIVO
        |
        |• Produtos virtuais (como a remoção permanente de anúncios) são adquiridos exclusivamente pela Google Play Store, que processa o pagamento;
        |• A compra de remoção de anúncios é vinculada à sua Conta Google e pode ser restaurada no próprio app através de "Restaurar compras";
        |• Reembolsos seguem as políticas da Google Play Store; solicitações podem ser registradas também no nosso suporte;
        |• Moedas e itens de progressão são fictícios, sem valor monetário real, não conversíveis e intransferíveis entre contas.
        |
        |5. LIMITAÇÃO DE RESPONSABILIDADE E GARANTIAS
        |
        |O TEMPOX é fornecido "no estado em que se encontra". Não garantimos funcionamento ininterrupto ou livre de erros em todos os dispositivos e versões do Android. Dentro dos limites da lei aplicável, o desenvolvedor não se responsabiliza por:
        |
        |• Perda de progresso local por reinstalação, troca de aparelho ou limpeza de dados do sistema;
        |• Indisponibilidade temporária de recursos dependentes de serviços de terceiros (anúncios, pagamentos);
        |• Danos indiretos decorrentes do uso do aplicativo.
        |
        |6. ALTERAÇÕES E ENCERRAMENTO
        |
        |Estes termos podem ser atualizados para refletir novos recursos ou exigências legais; a versão vigente estará sempre nesta página com data atualizada. O desenvolvedor pode descontinuar o serviço, mantendo o direito de uso das versões já instaladas.
        |
        |Suporte: bizflow.cloud@gmail.com · WhatsApp +258 840636794
    """.trimMargin()
}
