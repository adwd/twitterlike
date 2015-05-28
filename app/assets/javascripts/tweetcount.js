$(function(){
  var textarea = document.getElementById('tweetarea');
  Rx.Observable.fromEvent(textarea, 'input')
    .map(function(x) { return x.target.textLength; })
    .subscribe(function(length) {
      var tweetcount = document.getElementById('tweetcount');
      tweetcount.innerHTML = length;
    });
});
