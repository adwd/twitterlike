$ ->
  canvas = $("#canvas")[0]
  ctx = canvas.getContext('2d')

  initCanvas(canvas)

  # マウスイベントのRx化
  mouseDown = Rx.Observable.fromEvent($("#canvas"), 'mousedown')
  mouseMove = Rx.Observable.fromEvent($("#canvas"), 'mousemove')
  mouseUp = Rx.Observable.fromEvent($("#canvas"), 'mouseup')

  # 筆跡座標配列
  hisseki = []

  # 何画目か
  pressCount = 0

  mouseUp
    .subscribe( (x) ->
      console.log(hisseki.map (v, i) -> v.c + ', ' + v.x + ', ' + v.y)
      pressCount++
      $.ajax
        type : 'POST'
        url : '/api/moji'
        data : JSON.stringify(hisseki)
        contentType : 'application/json'
        success : dispMoji
        error : (err) -> console.log err
    )

  # マウスドラッグを画数と座標のストリームに変換
  mouseDrag = mouseDown
    .first()
    .concat(mouseMove.takeUntil(mouseUp))
    .map( (ev) -> {c: pressCount, x: ev.offsetX, y: ev.offsetY })
    .repeat()

  # hissekiに座標を追加
  mouseDrag
    .subscribe( (x) ->
      hisseki.push(x)
    )

  # 軌跡を描画
  Rx.Observable.zip(
      mouseDrag,
      mouseDrag.skip(1),
      (s1, s2) ->
        {s1, s2}
    )
    .subscribe(
      (line) ->
        drawline(ctx, line.s1, line.s2) if line.s1.c == line.s2.c
      (err) ->
        console.log
      () ->
        # oncompleteが起きないのはなぜ
        console.log 'completed'
        console.log arr
    )

  # クリアボタンですべてクリアする
  $('#tegaki-clear').click( () ->
    pressCount = 0
    hisseki = []
    [1..5].map (i) ->
      $("#moji" + i)
        .html("")

      initCanvas(canvas)
  )

  # モーダル表示参考URL: http://syncer.jp/jquery-modal-window
  # 手書きをモーダルポップアップで開く
  $("#modal-open").click( () ->
    $(this).blur()
    $("#modal-overlay").remove() if $("#modal-overlay")[0]
    $("body").append('<div id="modal-overlay"></div>')
    $("#modal-overlay").fadeIn("slow")
    $("#modal-content").fadeIn("slow")
  )

  # 内容を保存してモーダルを閉じる
  $("#modal-save").unbind().click( () ->
    $("#tweetarea")[0].value += $('#tegaki-text')[0].value
    $('#tegaki-text')[0].value = ""
    $("#modal-content,#modal-overlay").fadeOut("slow", () ->
      $("#modal-overlay").remove()
    )
  )

  # 内容を破棄してモーダルを閉じる
  $("#modal-overlay,#modal-close").unbind().click( () ->
    $('#tegaki-text')[0].value = ""
    $("#modal-content,#modal-overlay").fadeOut("slow", () ->
      $("#modal-overlay").remove()
    )
  )

  # 文字のクリックで入力を確定
  mojis = [1..5].map (i) -> $("#moji" + i)
  mojis.map( (v, i) ->
    v.click ->
      $('#tegaki-text')[0].value += v[0].innerHTML
      pressCount = 0
      hisseki = []
      [1..5].map (i) ->
        $("#moji" + i)
        .html("")

      initCanvas(canvas)
  )

initCanvas = (canvas) ->
  # 背景の300x300の白い四角
  ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(300, 0)
  ctx.lineTo(300, 300)
  ctx.lineTo(0, 300)
  ctx.closePath()
  ctx.stroke()
  ctx.fillStyle = 'rgb(255, 255, 255)'
  ctx.fill()

drawline = (ctx, p1, p2) ->
  ctx.strokeStyle = 'rbg(0, 0, 0)'
  ctx.strokeWidth = 5
  ctx.beginPath()
  ctx.moveTo(p1.x, p1.y)
  ctx.lineTo(p2.x, p2.y)
  ctx.closePath()
  ctx.stroke()

dispMoji = (x) ->
  # console.log(x.map (v, i) -> v.char + ': ' + v.score)
  x.map (v, i) ->
    if i < 6
      $("#moji" + i)
        .html(v.char)
        .css('font-size', 32)
        .css('background-color', "#FFFFFF")
        .css('margin', 4)

centeringModal = () ->
  w = $(window).width()
  h = $(window).height()
  cw = $("#modal-content").outerWidth({margin:true})
  ch = $("#modal-content").outerHeight({margin:true})
  pxleft = ((w - cw)/2)
  pxtop = ((h - ch)/2)
  $("#modal-content").css({"left": pxleft + "px"})
  $("#modal-content").css({"top": pxtop + "px"})

