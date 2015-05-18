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

import play.api.Play.current

object Application extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImpl {

  case class LoginForm(name: String, password: String)

  val loginForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply) verifying("ユーザー名またはパスワードが違います", form => form match {
      case login => validate(login)
    })
  )

  def validate(loginForm: LoginForm) = {
    DB.withSession { implicit session =>
      MemberTable
        .filter(_.memberId === loginForm.name)
        .firstOption
        .map(member => checkpw(loginForm.password, member.encryptedPassword))
        .getOrElse(false)
    }
  }

  case class RegisterForm(name: String, mail: String, password: String)

  val registerForm = Form(
    mapping(
      "name" -> nonEmptyText(maxLength = 30),
      "mail" -> email.verifying("40文字までのメールアドレスを入力してください", mail => mail.length < 40),
      "password" -> tuple("main" -> nonEmptyText(minLength = 2, maxLength = 20), "confirm" -> text).verifying(
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
      Tweets.showTweets(loggedIn.get)
    else {
      val recents = DB.withSession { implicit session =>
        TweetTable.sortBy(_.timestampCreated).take(10).list
      }
      Ok(views.html.index(loginForm, recents))
    }
  }

  /**
   * デバッグ表示、テーブルをすべて出力する
   */
  def debug = DBAction.transaction { implicit rs =>
    val members = MemberTable.sortBy(_.timestampCreated).list
    val tweets = TweetTable.sortBy(_.timestampCreated).list
    val follows = FollowTable.sortBy(_.timestampCreated).list

    Ok(views.html.debug(members, tweets, follows))
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
        val user = MemberTableRow(form.name, hashpw(form.password, gensalt()), form.mail, timestamp, timestamp)
        MemberTable.insert(user)
        Await.result(gotoLoginSucceeded(form.name), Duration.Inf)
      }
    )
  }

  def login = DBAction.transaction { implicit rs =>
    val recents = TweetTable.sortBy(_.timestampCreated).take(10).list
    Ok(views.html.index(loginForm, recents))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      error => {
        DB.withSession { implicit session =>
          val recents = TweetTable.sortBy(_.timestampCreated).take(10).list
          Future.successful(BadRequest(views.html.index(error, recents)))
        }
      },
      form => gotoLoginSucceeded(form.name)
    )
  }

  def follow(id: String) = StackAction { implicit request =>
    if(loggedIn.isDefined){
      val userId = loggedIn.get.memberId
      DB.withSession { implicit session =>
        if(MemberTable.filter(_.memberId === id).exists.run && !FollowTable.filter(flw => flw.memberId === userId && flw.followedId === id).exists.run) {
          val timestamp = new Timestamp(System.currentTimeMillis())
          val follow = FollowTableRow(0, id, userId, timestamp ,timestamp)
          FollowTable.insert(follow)
          Ok("follow success")
        } else {
          BadRequest("NG")
        }
      }
    } else {
      BadRequest("NG")
    }
  }

}