package controllers

import play.api.mvc.Controller

import controllers.Role.NormalUser

import jp.t2v.lab.play2.auth.AuthElement

object Tweets extends Controller with AuthElement with AuthConfigImpl {

  def main = StackAction(AuthorityKey -> NormalUser) { implicit request =>
    //val user = loggedIn
    Ok(views.html.twitterlike.tweets())
  }
}