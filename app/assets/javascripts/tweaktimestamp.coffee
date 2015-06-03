# データベースではUTCで保存されていて、読みだす時にサーバーのローカル時になる。
# それをクライアントのローカル時にして、フォーマットを2015-05-15 12:22:34:0を5/15 12:22にする
format = (timestamp) ->
  localDate = new Date()
  localTime = localDate.getTime()
  localOffset = localDate.getTimezoneOffset() * 60000

  # serverはUTCで動いている
  serverDate = new Date(timestamp.innerText)
  serverTime = serverDate.getTime()

  d = new Date(serverTime - localOffset)
  timestamp.innerText = new Date(d).toLocaleString()

$ ->
  format timestamp for timestamp in $(".tweet-time")