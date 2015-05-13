package controllers

import java.sql.Timestamp

import jp.t2v.lab.play2.auth.{OptionalAuthElement, LoginLogout}
import play.api.mvc._
import play.api.db.slick._
import models.Tables._
import profile.simple._

import play.api.data._
import play.api.data.Forms._

import org.mindrot.jbcrypt.BCrypt.{hashpw, checkpw, gensalt}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Application extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl {

  case class LoginForm(name: String, password: String)

  val loginForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  )

  case class RegisterForm(name: String, mail: String, password: String)

  val registerForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "mail" -> email,
      "password" -> tuple("main" -> nonEmptyText(minLength = 8), "confirm" -> text).verifying(
        "Passwords don't match", passwords => passwords._1 == passwords._2
      )
    ){ (name, mail, passwords) => RegisterForm(name, mail, passwords._1)}
    { form => Some(form.name, form.mail, (form.password, form.password))}
  )

  /**
   * ログインしていたらメイン画面、ログインしていなければログイン画面
   * @return
   */
  def index = StackAction { implicit request =>
    if(loggedIn.isDefined)
      Ok(views.html.twitterlike.tweets())
    else
      Ok(views.html.index(loginForm))
  }

  /**
   * デバッグ表示、テーブルをすべて出力する
   */
  def debug = DBAction.transaction { implicit rs =>
    val members = MemberTable.sortBy(_.timestampCreated).list

    Ok(views.html.debug(members))
  }

  /**
   * 会員登録ページ表示
   */
  def registry = Action {
    Ok(views.html.registry(registerForm))
  }

  /**
   * 登録実行
   */
  def create = DBAction.transaction { implicit rs =>
    registerForm.bindFromRequest.fold(
      error => BadRequest(views.html.registry(error)),
      form => {
        // ユーザを登録
        val timestamp = new Timestamp(System.currentTimeMillis())
        val user = MemberTableRow(form.name, None, hashpw(form.password, gensalt()), form.mail, timestamp, timestamp)
        MemberTable.insert(user)
        Await.result(gotoLoginSucceeded(form.name), Duration.Inf)
      }
    )
  }

  def login = DBAction.transaction { implicit rs =>
    Ok(views.html.index(loginForm))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      error => Future.successful(Redirect(routes.Tweets.main())),
      form => gotoLoginSucceeded(form.name)
    )
  }

}