package controllers

import controllers.Role.{NormalUser, Administrator}
import jp.t2v.lab.play2.auth.{CookieTokenAccessor, AuthConfig}

import models.Tables._
import play.Logger

import play.api.mvc._
import play.api.mvc.Results._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json._

import scala.reflect._
import scala.concurrent.{ExecutionContext, Future}

import profile.simple._

/**
 * サイトにログインするメンバーの権限として一般会員と管理者を設定
 */
sealed trait Role

object Role {

  case object Administrator extends Role
  case object NormalUser extends Role

  def valueOf(value: String): Role = value match {
    case "Administrator" => Administrator
    case "NormalUser"    => NormalUser
    case _ => throw new IllegalArgumentException()
  }

}

trait AuthConfigImplCommon extends AuthConfig {

  /**
   * ユーザを識別するIDの型です。String や Int や Long などが使われるでしょう。
   */
  type Id = String

  /**
   * あなたのアプリケーションで認証するユーザを表す型です。
   * User型やAccount型など、アプリケーションに応じて設定してください。
   */
  type User = MemberTableRow

  /**
   * 認可(権限チェック)を行う際に、アクション毎に設定するオブジェクトの型です。
   * このサンプルでは例として以下のような trait を使用しています。
   *
   * sealed trait Role
   * case object Administrator extends Role
   * case object NormalUser extends Role
   */
  type Authority = Role

  /**
   * CacheからユーザIDを取り出すための ClassTag です。
   * 基本的にはこの例と同じ記述をして下さい。
   */
  val idTag: ClassTag[Id] = classTag[Id]

  /**
   * セッションタイムアウトの時間(秒)です。
   */
  val sessionTimeoutInSeconds: Int = 3600

  /**
   * ユーザIDからUserブジェクトを取得するアルゴリズムを指定します。
   * 任意の処理を記述してください。
   */
  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = {
    val user = DB.withSession { implicit session =>
      MemberTable.filter(_.memberId === id).firstOption
    }

    Future.successful(user)
  }

  /**
   * 互換性の為に残されているメソッドです。
   * 将来のバージョンでは取り除かれる予定です。
   * authorizationFailed(RequestHeader, User, Option[Authority]) を override してください。
   */
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = throw new AssertionError

  /**
   * 権限チェックのアルゴリズムを指定します。
   * 任意の処理を記述してください。
   */
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    (/* user.role */ NormalUser.asInstanceOf[Role], authority) match {
      case (Administrator, _) => true
      case (NormalUser, NormalUser) => true
      case _ => false
    }
  }

  /**
   * (Optional)
   * SessionID Tokenの保存場所の設定です。
   * デフォルトでは Cookie を使用します。
   */
  override lazy val tokenAccessor = new CookieTokenAccessor(
    /*
     * cookie の secureオプションを使うかどうかの設定です。
     * デフォルトでは利便性のために false になっていますが、
     * 実際のアプリケーションでは true にすることを強く推奨します。
     */
    cookieSecureOption = play.api.Play.isProd(play.api.Play.current),
    cookieMaxAge       = Some(sessionTimeoutInSeconds)
  )
}

trait AuthConfigImplHtml extends AuthConfigImplCommon {
  /**
   * ログインが成功した際に遷移する先を指定します。
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"login succeeded $request")
    Future.successful(Redirect(routes.Tweets.main()))
  }

  /**
   * ログアウトが成功した際に遷移する先を指定します。
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"logout succeeded $request")
    Future.successful(Redirect(routes.Application.index()))
  }

  /**
   * 認証が失敗した場合に遷移する先を指定します。
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"authentication failed $request")
    Future.successful(Redirect(routes.Application.index()))
  }

  /**
   * 認可(権限チェック)が失敗した場合に遷移する先を指定します。
   */
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden("no permission"))
  }
}

trait AuthConfigImplJson extends AuthConfigImplCommon {
  /**
   * ログインが成功した際に遷移する先を指定します。
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"login succeeded $request")
    Future.successful(Ok(Json.obj("status" -> "OK", "message" -> "login succeeded")))
  }

  /**
   * ログアウトが成功した際に遷移する先を指定します。
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"logout succeeded $request")
    Future.successful(Ok(Json.obj("status" -> "OK", "message" -> "logout succeeded")))
  }

  /**
   * 認証が失敗した場合に遷移する先を指定します。
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    Logger.debug(s"authentication failed $request")
    Future.successful(BadRequest(Json.obj("status" -> "NG", "message" -> "authentication failed")))
  }

  /**
   * 認可(権限チェック)が失敗した場合に遷移する先を指定します。
   */
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden(Json.obj("status" -> "NG", "message" -> "authorization failed")))
  }

}