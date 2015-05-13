
function addListener(elem, ev, listener) {
  if(elem.addEventListener) {
    elem.addEventListener(ev, listener, false);
  } else if(elem.attachEvent) {
    elem.attachEvent('on' + ev, listener);
  } else {
    throw new Error('イベントリスナに未対応です。');
  }
}
addListener(window, 'load', init);

function init() {
  // id='tweetarea'のテキストエリアの内容が変化した時に
  // 入力されている文字数をid='tweetcount'のinnerHTMLに表示する
  var textarea = document.getElementById('tweetarea');
  Rx.Observable.fromEvent(textarea, 'input')
    .map(function(x) { return x.target.textLength })
    .subscribe(function(length) {
      var tweetcount = document.getElementById('tweetcount');
      tweetcount.innerHTML = length;
    });
}
