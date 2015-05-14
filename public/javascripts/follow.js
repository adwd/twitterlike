/**
 * フォローボタンを押して、フォローボタンを無効化する
 * @param id
 */
function disableButton(id) {
  var buttonId = '.follow-button#' + id;
  $(buttonId).prop("disabled", true);
}

/**
 * フォローされたユーザーをリストに追加する
 */
function appendButton(id) {

  //$("#following-user-table").insertRow();
}

/**
 * follow-buttonクラスのbuttonをクリックすると、idに設定された文字列を使って/follow/:idにPOSTする
 */
$(
  function() {
    $('.follow-button').click(
      function() {
        var userToFollow = this.id;
          $.post("/follow/" + userToFollow)
            .done(function() {
              disableButton(userToFollow);
              appendButton(userToFollow);
            })
            .fail(function() { alert("フォローできませんでした。すでにフォローしているかもしれません。"); });
      }
    );
  }
);