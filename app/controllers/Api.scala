package  controllers

import java.sql.Timestamp

import controllers.Role.NormalUser
import controllers.Tweets._
import jp.t2v.lab.play2.auth.{AuthElement, OptionalAuthElement, LoginLogout}
import play.api.mvc._
import play.api.db.slick._
import models.Tables._
import play.api.libs.json._
import profile.simple._

import org.mindrot.jbcrypt.BCrypt.{hashpw, checkpw, gensalt}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Play.current
import controllers.Application._

object Api extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl {

  implicit val tweetWrites = Json.writes[TweetTableRow]
  implicit val loginReads = Json.reads[LoginForm]
  implicit val memberWrites = Json.writes[MemberTableRow]

  def loginValidate(jsValue: JsValue): JsResult[LoginForm] = {
    loginReads.reads(jsValue).map( form =>
      DB.withSession{ implicit session =>
        MemberTable
          .filter(_.memberId === form.name)
          .firstOption.find(member => checkpw(form.password, member.encryptedPassword))
          .map(member => JsSuccess(form))
          .getOrElse(JsError(Seq()))
    }).getOrElse(JsError(Seq()))
  }

  def authenticate = Action.async(BodyParsers.parse.json) { implicit request =>
      loginValidate(request.body).fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "NG", "message" -> "authenticate failed."))),
      login => gotoLoginSucceeded(login.name)
    )
  }

  def recents = DBAction { implicit session =>
    val tweets = TweetTable.sortBy(_.timestampCreated).take(10).list
    Ok(Json.toJson(tweets))
  }

  def tweets = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    loggedIn.map{ user =>
      val (tw, _, _) = tweetImpl(loggedIn.get)
      Ok(Json.toJson(tw))
    }.getOrElse{
      BadRequest(Json.obj("status" -> "NG", "message" -> "login first."))
    }
  }

  def follow(name: String) = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    loggedIn.map{ user =>
      DB.withSession { implicit session =>
        TweetTable
          .filter(_.memberId === name)
          .firstOption
          .map { member =>
            val timestamp = new Timestamp(System.currentTimeMillis())
            val follow = FollowTableRow(0, name, user.memberId, timestamp, timestamp)
            FollowTable.insert(follow)
            Ok(Json.obj("status" -> "OK", "message" -> s"followed ${member.memberId}"))}
          .getOrElse(BadRequest(Json.obj("status" -> "NG", "message" -> s"user: $name not found.")))
      }
    }.getOrElse{
      BadRequest(Json.obj("status" -> "NG", "message" -> "login first."))
    }
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def tweet = StackAction(parse.json, AuthorityKey -> NormalUser) { implicit req =>
    loggedIn.map{ user =>
      val text = req.body.\("text").as[String]
      val timestamp = new Timestamp(System.currentTimeMillis())
      val tweet = TweetTableRow(0, Some(text), user.memberId, timestamp, timestamp)
      DB.withSession( implicit session => TweetTable.insert(tweet))
      val (tw, _, _) = tweetImpl(user)
      Ok(Json.toJson(tw))
    }.getOrElse(BadRequest(Json.obj("status" -> "NG", "message" -> "tweet failed.")))
  }

  def recommends = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    loggedIn.map { user =>
      val (_, recommends, _) = tweetImpl(user)
      Ok(Json.toJson(recommends))
    }.getOrElse(BadRequest(Json.obj("status" -> "NG", "message" -> "failed to recommend.")))
  }
}