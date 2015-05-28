function followed(id) {
  var button = $('.follow-button#' + id);
  button.prop("disabled", true);
  button[0].textContent = "フォローしました";
}

function unfollowed(id) {
  var button = $('.unfollow-button#' + id);
  button.prop("disabled", true);
  button[0].textContent = "アンフォローしました";
}

$(
  function() {
    $('.follow-button').click(
      function() {
        var userToFollow = this.id;
        $.post("/follow/" + userToFollow)
          .done(function() {
            followed(userToFollow);
          })
          .fail(function() { alert("フォローに失敗しました。"); });
      }
    );

    $('.unfollow-button').click(
      function() {
        var userToUnfollow = this.id;
        $.post("/unfollow/" + userToUnfollow)
          .done(function() {
            unfollowed(userToUnfollow);
          })
          .fail(function() { alert("アンフォローに失敗しました。"); });
      }
    );
  }
);