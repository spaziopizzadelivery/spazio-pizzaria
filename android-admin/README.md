# Spazio Admin — Android (primeira etapa)
Aplicativo Android exclusivo da administração Spazio Pizza.

## O que já está implementado
- APK Android com painel administrativo existente, login Supabase e acesso às alterações compartilhadas com o site.
- Nenhuma senha nem chave secreta embutida no aplicativo.
- Build automático do APK de teste pelo GitHub Actions (workflow **Build Android Admin APK**).

## Como obter o APK de teste
GitHub > Actions > Build Android Admin APK > Run workflow. Após a execução, baixar o artefato **Spazio-Admin-APK-debug**.

## Importante: notificações em segundo plano AINDA NÃO implementadas
O alerta sonoro atual depende do painel aberto. Para alertar com o app fechado ou em segundo plano, a próxima etapa exige Firebase Cloud Messaging, credenciais de projeto Firebase e um emissor de notificações seguro no backend (Supabase Edge Function/trigger). Não confundir o APK inicial com a versão pronta para operação comercial.

## Integração
O aplicativo exibe o mesmo admin.html do GitHub Pages. Produtos, preços, pedidos e configurações são persistidos no Supabase compartilhado. A alteração de layout do painel web também aparece no aplicativo após atualizar a página.

## Segurança
Usar autenticação e RLS do Supabase. Não colocar service_role no app. Não distribuir APK de depuração como versão final.
