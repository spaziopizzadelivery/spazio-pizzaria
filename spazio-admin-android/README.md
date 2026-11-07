# Spazio Admin Android

Aplicativo Android nativo para administrar a Spazio Pizza Delivery, conectado ao mesmo projeto Supabase utilizado pelo painel web.

## Recursos
- Login Supabase e verificação na tabela `admin_users`
- Pedidos: listagem, dados do cliente, itens, valores, aceitar/rejeitar e atualizar status
- Produtos: listar, criar, editar, ativar/desativar
- Bordas: listar, criar, editar, ativar/desativar
- Configurações: listar, criar e editar valores JSON
- Atualização automática de pedidos a cada 15 segundos enquanto o app está aberto
- Notificação local quando novos pedidos chegam enquanto o app está em primeiro plano

## Build
O workflow `.github/workflows/build-spazio-admin.yml` compila o APK de depuração e publica um artefato e uma release GitHub.
