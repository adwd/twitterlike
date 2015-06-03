$ ->
  $('#login-button').prop("disabled", true)

  # ログインの名前とパスワードの入力イベントをそれぞれ入力文字数のストリームにする
  streams = $.map(['#login-name', '#login-password'], (n, i) ->
    Rx.Observable.fromEvent($(n), 'input').map( (x) ->
      x.target.value.length
    )
  )

  # 名前とパスワードのストリームを一つのオブジェクトのストリームにして
  # ボタンのdisabledプロパティに反映
  Rx.Observable.combineLatest(streams[0], streams[1], (s1, s2) ->
    {'name': s1, 'pass': s2}
  ).subscribe( (x) ->
    $('#login-button').prop("disabled", x.name == 0 || x.pass < 8)
  )
