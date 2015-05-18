package  controllers

import java.sql.Timestamp

import controllers.Role.NormalUser
import controllers.Tweets._
import jp.t2v.lab.play2.auth.{AuthElement, OptionalAuthElement, LoginLogout}
import play.api.libs.json.{JsPath, Writes}
import play.api.libs.functional.syntax._
import play.api.mvc._
import play.api.db.slick._
import models.Tables._
import play.api.libs.json._
import profile.simple._

import play.api.data._
import play.api.data.Forms._

import org.mindrot.jbcrypt.BCrypt.{hashpw, checkpw, gensalt}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import play.api.Play.current

import controllers.Application._

object Api extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl {

  implicit val tweetWrites = Json.writes[TweetTableRow]
  implicit val loginReads = Json.reads[LoginForm]

  def loginValidate(jsValue: JsValue): JsResult[LoginForm] = {
    val form = loginReads.reads(jsValue).get
    val isValid = DB.withSession{ implicit session =>
      MemberTable
        .filter(_.memberId === form.name)
        .firstOption
        .map(member => checkpw(form.password, member.encryptedPassword))
        .getOrElse(false)
    }

    if(isValid)
      JsSuccess(form)
    else
      JsError(Seq())
  }

  def authenticate = Action.async(BodyParsers.parse.json) { implicit request =>
      loginValidate(request.body).fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "NG", "message" -> "authenticate failed."))),
      login => gotoLoginSucceeded(login.name)
    )
  }

  def recents = StackAction { implicit request =>
    val recents = DB.withSession { implicit session =>
      TweetTable.sortBy(_.timestampCreated).take(10).list
    }
    val json = Json.toJson(recents)
    Ok(json)
  }

  def tweets = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    if(loggedIn.isDefined){
      val (tw, rec, fl) = tweetImpl(loggedIn.get)
      Ok(Json.toJson(tw))
    } else {
      BadRequest(Json.obj("status" -> "NG", "message" -> "login first."))
    }
  }
}