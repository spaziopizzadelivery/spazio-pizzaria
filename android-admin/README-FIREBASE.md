# Firebase Cloud Messaging — integração inicial
Projeto Firebase: spazio-pizza-admin
Pacote Android: com.spaziopizza.admin

O app agora inclui google-services.json, Google Services Gradle plugin e Firebase Messaging.
O serviço OrderMessagingService recebe mensagens FCM do tipo data e exibe uma notificação de alta prioridade.

## Pendências obrigatórias antes de funcionar de ponta a ponta
1. Solicitar permissão POST_NOTIFICATIONS em tempo de execução (Android 13+).
2. Registrar e atualizar o token FCM de cada dispositivo no Supabase somente após login de administrador, com RLS apropriada.
3. Implementar disparo server-side seguro quando novos pedidos forem inseridos, com Firebase Admin SDK ou HTTP v1 usando credenciais guardadas no backend.
4. Testar recebimento em primeiro plano, segundo plano, app encerrado, tela bloqueada e reconexão.
5. Testar a compilação do APK no GitHub Actions.
6. Para alarme repetitivo até decisão, implementar fluxo de alerta Android próprio e avaliar restrições de execução em segundo plano.

Não armazenar chave de conta de serviço Firebase no repositório nem no aplicativo.
