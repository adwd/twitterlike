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
import play.api.data._
import play.api.data.Forms._
import org.mindrot.jbcrypt.BCrypt.{hashpw, checkpw, gensalt}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Play.current
import controllers.Application._

object Api extends Controller with LoginLogout with AuthElement with AuthConfigImplJson {

  implicit val tweetWrites = Json.writes[TweetTableRow]
  implicit val loginReads = Json.reads[LoginForm]
  implicit val memberWrites = Json.writes[MemberTableRow]

  def loginValidate(jsValue: JsValue): JsResult[LoginForm] = {
    loginReads.reads(jsValue).map( form =>
      DB.withSession{ implicit session =>
        MemberTable
          .filter(_.memberId === form.name)
          .firstOption
          .find(member => checkpw(form.password, member.encryptedPassword))
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
    val (tw, _, _) = tweetImpl(loggedIn)
    Ok(Json.toJson(tw))
  }

  def follow(name: String) = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    DB.withSession { implicit session =>
      TweetTable
        .filter(_.memberId === loggedIn.memberId)
        .firstOption
        .map { member =>
          val timestamp = new Timestamp(System.currentTimeMillis())
          val follow = FollowTableRow(0, name, loggedIn.memberId, timestamp, timestamp)
          FollowTable.insert(follow)
          Ok(Json.obj("status" -> "OK", "message" -> s"followed ${member.memberId}"))
        }.getOrElse(BadRequest(Json.obj("status" -> "NG", "message" -> s"user: $name not found.")))
    }
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def tweet = StackAction(parse.json, AuthorityKey -> NormalUser) { implicit req =>
    req.body.\("text").asOpt[String].map { (text: String) =>
      val timestamp = new Timestamp(System.currentTimeMillis())
      val tweet = TweetTableRow(0, Some(text), loggedIn.memberId, timestamp, timestamp)
      DB.withSession(implicit session => TweetTable.insert(tweet))
      val (tw, _, _) = tweetImpl(loggedIn)
      Ok(Json.toJson(tw))
    }.getOrElse(BadRequest(Json.obj("status" -> "NG", "message" -> "path 'text' required")))
  }

  def recommends = StackAction(AuthorityKey -> NormalUser) { implicit req =>
    val (_, recommends, _) = tweetImpl(loggedIn)
    Ok(Json.toJson(recommends))
  }

  case class MemberData(name: String, pass: String, mail: String)
  val memberDataConstraints = Form(
    mapping(
      "name" -> nonEmptyText.verifying("user name must be less than 30 characters", _.length < 30),
      "pass" -> nonEmptyText,
      "mail" -> email.verifying("Email address must be less than 40 characters", _.length <= 40)
    )(MemberData.apply)(MemberData.unapply)
  )

  def create = DBAction.transaction(parse.json) { implicit request =>
    def isMemberNameUnique(name: String): Either[String, String] = {
      MemberTable
        .filter(_.memberId === name)
        .firstOption
        .map(m => "user name already exists")
        .toLeft("user name available")
    }

    def memberDataValidate(n: String, p: String, m: String): Either[String, String] = {
      memberDataConstraints.bind(Map("name" -> n, "pass" -> p, "mail" -> m)).fold(
        error => Left(error.errors.map(_.message).mkString(", ")),
        form => Right(n))
    }

    val values = Seq("name", "password", "mail")
      .map(request.body.\(_).asOpt[String])

    val insertMember: Either[String, String] = for{
      name      <- values(0).toRight("invalid json. path 'name' required.").right
      password  <- values(1).toRight("invalid json. path 'password' required.").right
      mail      <- values(2).toRight("invalid json. path 'mail' required.").right
      _         <- isMemberNameUnique(name).right
      _         <- memberDataValidate(name, password, mail).right
    } yield {
      val timestamp = new Timestamp(System.currentTimeMillis())
      val user = MemberTableRow(name, hashpw(password, gensalt()), mail, timestamp, timestamp)
      MemberTable.insert(user)
      user.memberId
    }

    insertMember
      .fold(
        errorMessage => BadRequest(Json.obj("status" -> "NG", "message" -> errorMessage)),
        memberName => Await.result(gotoLoginSucceeded(memberName), Duration.Inf))
  }
}