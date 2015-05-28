$ ->
  # ツイート欄の文字数ストリーム
  textLengthStream = Rx.Observable.fromEvent($('#tweetarea'), 'input')
    .map( (x) -> x.target.textLength )

  # 文字数の表示と、140文字を超える場合の処理
  textLengthStream
  .subscribe((length) ->
    $('#tweetcount')
      .html(length)
      .css('color', if(length > 140) then 'red' else 'black')
      .css('font-weight', if(length > 140) then 'bold' else 'normal')

    $('#button-tweet').prop("disabled", length > 140)
  )
