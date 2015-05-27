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

object Tweets extends Controller with AuthElement with AuthConfigImplHtml {

  case class TweetForm(tweet: String, memberId: String)

  val tweetForm = Form(
    mapping("tweet" -> text(maxLength = 140), "memberId" -> text)(TweetForm.apply)(TweetForm.unapply)
  )

  def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    showTweets(loggedIn)
  }

  def showTweets[T](user: MemberTableRow)(implicit request: Request[T]) = {
      val (tw, rec, fl) = tweetImpl(user)
      Ok(views.html.tweets(user, tw, rec, fl, tweetForm.fill(TweetForm("", user.memberId))))
  }

  def tweetImpl(user: User) = {
    DB.withSession { implicit session =>
      // TODO: ちゃんとしたSQLクエリにするとか

      val followings = for {
        follow <- FollowTable
        if follow.memberId === user.memberId
      } yield follow

      val members = {
        for {
          member <- MemberTable
          isfollowed = member.memberId.inSet(followings.map(_.followedId).list)
        } yield (member, isfollowed)
      }

      val tweets = TweetTable
        .filter(tweet => tweet.memberId === user.memberId || followings.map(_.followedId).filter(tweet.memberId === _).exists)
        .list

      val recommends = members
        .filter(_._2 === false)
        .map(_._1)
        .filter(_.memberId =!= user.memberId)
        .list
      val followed = members
        .filter(_._2 === true)
        .map(_._1)
        .list

      (tweets, recommends, followed)
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