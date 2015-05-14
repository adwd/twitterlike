package controllers

import java.sql.Timestamp

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.db.slick._
import play.api.Play.current

import controllers.Role.NormalUser
import models.Tables._

import profile.simple._

import jp.t2v.lab.play2.auth.AuthElement

object Tweets extends Controller with AuthElement with AuthConfigImpl {

  case class TweetForm(tweet: String, memberId: String)

  val tweetForm = Form(
    mapping("tweet" -> text(maxLength = 140), "memberId" -> text)(TweetForm.apply)(TweetForm.unapply)
  )

  def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    showTweets(loggedIn)
  }

  def showTweets[T](user: MemberTableRow)(implicit request: Request[T]) = {
    DB.withSession { implicit session =>
      // TODO: ちゃんとしたSQLクエリにするとか
      // FollowTableのフォローしているユーザーが現在のユーザーである行
      val followingMember = FollowTable
        .filter(_.memberId === user.memberId)
        .list
      val tweets = TweetTable
        .filter(tweet => tweet.memberId === user.memberId || followingMember.map(_.followedId).exists(tweet.memberId == _))
        .list
      // MemberTableのmemberIdが、FollowTableのfollowedIdに含まれていない
      val recommended = MemberTable
        .withFilter(member => !followingMember.map(_.followedId).contains(member.memberId))
        .filterNot(_.memberId === user.memberId)
        .take(5)
        .list
      val followingmembers = MemberTable
        .withFilter(member => followingMember.map(_.followedId).contains(member.memberId))
        .take(10)
        .list

      Ok(views.html.twitterlike.tweets(user, tweets, recommended, followingmembers, tweetForm.fill(TweetForm("", user.memberId))))
    }
  }

  def tweet = DBAction { implicit request =>
    val form = tweetForm.bindFromRequest
    val timestamp = new Timestamp(System.currentTimeMillis())
    val tweet = TweetTableRow(0, Some(form.get.tweet), form.get.memberId, timestamp, timestamp)
    TweetTable.insert(tweet)
    Redirect(routes.Tweets.main())
  }
}