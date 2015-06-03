# データベースではUTCで保存されていて、読みだす時にサーバーのローカル時になる。
# それをクライアントのローカル時にして、フォーマットを2015-05-15 12:22:34:0を5/15 12:22にする
format = (timestamp) ->
  clientdate = new Date()
  clientOffset = clientdate.getTimezoneOffset()
  serverdate = new Date(timestamp.innerText)
  serveroffset = serverdate.getTimezoneOffset()
  servertime = serverdate.getTime()
  result = servertime + (clientOffset - serveroffset) * 60000
  d = new Date(result)
  timestamp.innerText = d.toLocaleString()

$ ->
  format timestamp for timestamp in $(".tweet-time")