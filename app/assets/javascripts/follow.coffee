# 押したボタンを押せなくする
followed = (id) ->
  $('.follow-button#' + id)
    .prop('disabled', true)
    .css('color', 'gray')
    .html("followed")

unfollowed = (id) ->
  $('.unfollow-button#' + id)
    .prop('disabled', true)
    .css('color', 'gray')
    .html('unfollowed')

# followとunfollowボタンでPOSTする
$ ->
  $('.follow-button').click( () ->
    id = this.id
    $.post('/follow/' + this.id)
      .done( () ->
        followed(id)
      ).fail( () ->
        alert('フォローに失敗しました。')
    )
  )

  $('.unfollow-button').click( () ->
    id = this.id
    $.post('/unfollow/' + this.id)
      .done( () ->
        unfollowed(id)
      ).fail( () ->
        alert('アンフォローに失敗しました。')
    )
  )