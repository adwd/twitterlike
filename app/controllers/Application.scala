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

object Application extends Controller with LoginLogout with OptionalAuthElement with AuthConfigImplHtml {

  case class LoginForm(name: String, password: String)

  val loginForm = Form(
    mapping(
      "name" -> text,
      "password" -> text
    )(LoginForm.apply)(LoginForm.unapply) verifying("ユーザー名またはパスワードが違います", form => form match {
      case login => validate(login)
    })
  )

  def validate(loginForm: LoginForm) = {
    DB.withSession { implicit session =>
      MemberTable
        .filter(_.memberId === loginForm.name)
        .firstOption
        .fold(false)(member => checkpw(loginForm.password, member.encryptedPassword))
    }
  }

  case class RegisterForm(name: String, mail: String, password: String)

  val registerForm = Form(
    mapping(
      "name" -> text
        .verifying("16文字までの名前を入力してください", n => !n.isEmpty && n.length <= 16)
        .verifying("英数字、ハイフン、アンダーバーのみ使用できます", _.matches("""[a-zA-Z0-9_]+"""))
        .verifying("既に使われているユーザー名です", name => DB.withSession{ implicit session =>
          !MemberTable.filter(_.memberId === name).exists.run
        }),
      "mail" -> text
        .verifying("40文字までのメールアドレスを入力してください", m => !m.isEmpty && m.length < 40)
        .verifying("不正なE-mailアドレスです", _.matches("[\\w\\d_-]+@[\\w\\d_-]{2,}\\.[\\w\\d._-]{2,}")),
      "password" ->
          tuple(
            "main" -> text
              .verifying("2文字以上 20文字までのパスワードを入力してください", p => p.length >= 2 && p.length <= 20),
            "confirm" -> text
          ).verifying("パスワードが一致しません", pw => pw._1 == pw._2
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
    loggedIn.fold(BadRequest("NG")){ user =>
      DB.withSession { implicit session =>
        if(MemberTable.filter(_.memberId === id).exists.run && !FollowTable.filter(flw => flw.memberId === user.memberId && flw.followedId === id).exists.run) {
          val timestamp = new Timestamp(System.currentTimeMillis())
          val follow = FollowTableRow(0, id, user.memberId, timestamp ,timestamp)
          FollowTable.insert(follow)
          Ok("follow success")
        } else {
          BadRequest("NG")
        }
      }
    }
  }

  def unfollow(id: String) = StackAction { implicit request =>
    loggedIn.fold(BadRequest("you are not logged in"))( user =>
      DB.withSession{ implicit session =>
        MemberTable
          .filter(_.memberId === id)
          .firstOption
          .fold(BadRequest(s"$user is not exist.")){ x =>
            val ret = FollowTable
              .filter(f => f.memberId === user.memberId &&
                           f.followedId === id)
              .delete
            if (ret != 0) {
              Ok("unfollow success")
            } else {
              BadRequest(s"you are not following $id")
            }
        }
      })
  }

}