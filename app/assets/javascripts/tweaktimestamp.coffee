# 2015-05-15 12:22:34:0を5/15 12:22にする
format = (timestamp) ->
  timestamp.innerText = timestamp.innerText.substr(5,11).replace("-", "/").replace(/^0/g, "")

$ ->
  format timestamp for timestamp in $(".tweet-time")