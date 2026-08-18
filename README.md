# HylorVanish — Vanish Staff

HylorVanish é um plugin de vanish desenvolvido para servidores Minecraft, focado em **moderação**, **segurança** e **facilidade de uso**. O sistema permite que membros da equipe fiquem completamente invisíveis para jogadores comuns sem utilizar efeitos de poção.

## Funcionalidades

* ✅ Vanish real utilizando o sistema de ocultação de jogadores.
* ✅ Staffs conseguem enxergar outros staffs em vanish.
* ✅ Jogadores comuns não conseguem visualizar staffs ocultos.
* ✅ Staff continua podendo interagir normalmente com jogadores e o servidor.
* ✅ ActionBar permanente: `&d&lMODO VANISH`.
* ✅ Vanish persistente após relogar ou reiniciar o servidor.
* ✅ Mensagens totalmente configuráveis.
* ✅ Estado dos jogadores armazenado em `vanished.yml`.
* ✅ Não utiliza poção de invisibilidade.

## Comandos

| **Comando** | **Função**              |
| ----------- | ----------------------- |
| `/v`        | Ativa o modo Vanish.    |
| `/vv`       | Desativa o modo Vanish. |

## Permissões

| **Permissão**       | **Função**                                                                |
| ------------------- | ------------------------------------------------------------------------- |
| `hylorvanish.staff` | Permite utilizar o Vanish e visualizar outros staffs que estejam ocultos. |

## Funcionamento

```text
Staff executa /v
       ↓
HylorVanish ativa o modo Vanish
       ↓
Jogadores comuns deixam de visualizar o staff
       ↓
Staffs com hylorvanish.staff continuam enxergando
       ↓
ActionBar → MODO VANISH
       ↓
Vanish permanece ativo
       ↓
/vv → Vanish desativado
```

## Persistência

Uma vez que o staff entra no Vanish, ele **não sai automaticamente**. Desconectar ou reiniciar o servidor não remove seu estado.

O Vanish somente é desativado quando o próprio staff utiliza:

`/vv`

## Compatibilidade

Desenvolvido para servidores **Spigot/Paper/PandaSpigot**, com foco em servidores de Minecraft **1.8.x**.

## About

Plugin de vanish para equipes de servidores Minecraft, desenvolvido para fornecer um sistema simples, persistente e seguro de moderação invisível.
