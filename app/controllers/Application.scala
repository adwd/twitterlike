package controllers

import java.sql.Timestamp

import play.api.mvc._
import play.api.db.slick._
import models.Tables._
import profile.simple._

import play.api.data._
import play.api.data.Forms._

import org.mindrot.jbcrypt.BCrypt.{hashpw, checkpw, gensalt}

object Application extends Controller {
  val salt = """$2a$10$zXdoVN2Xci3bRB8UwnEL7u"""

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
    ){(name, mail, passwords) => RegisterForm(name, mail, passwords._1)}
    { form => Some(form.name, form.mail, (form.password, ""))}
  )

  def index = Action {
    Ok(views.html.index(registerForm))
  }

  /**
   * デバッグ表示、テーブルをすべて出力する
   */
  def debug = DBAction.transaction { implicit rs =>
    val members = MemberTable.sortBy(_.timestampCreated).list

    Ok(views.html.debug(members))
  }

  /**
   * 登録実行
   */
  def create = DBAction.transaction { implicit rs =>
    registerForm.bindFromRequest.fold(
      error => Redirect(routes.Application.index),
      form => {
        // ユーザを登録
        val timestamp = new Timestamp(System.currentTimeMillis())
        val user = MemberTableRow(form.name, None, hashpw(form.password, salt), form.mail, timestamp, Some(timestamp))
        MemberTable.insert(user)

        Redirect(routes.Application.debug)
      }
    )
  }

  def login = DBAction.transaction { implicit rs =>
    loginForm.bindFromRequest.fold(
      error => Redirect(routes.Application.index),
      form => {
        Redirect(routes.Application.index)
      }
    )
  }

}