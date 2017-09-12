package controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import me.yangbajing.ps.business.service.CacheService
import me.yangbajing.ps.data.domain.OwnerToken
import me.yangbajing.ps.util.Constants
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Cookie, DiscardingCookie, RequestHeader}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random

/**
  * cache放到redis里。
  */
@Singleton
class WebTools @Inject()(cacheService: CacheService) {
  /**
    *
    * @param request
    * @return
    * case Some(_) =>
    * future map {
    * case Right(_) => 成功获取到OwnerToken
    * case Left(key) => key无效
    * }
    * case None => token key's value 未找到
    */
  def getUserToken(implicit request: RequestHeader): Option[Either[String, OwnerToken]] = {
    val str = request.cookies.find(_.name == Constants.PS_TOKEN_OWNER).map(_.value) orElse
      request.headers.get(Constants.PS_TOKEN_OWNER) orElse
      request.getQueryString(Constants.PS_TOKEN_OWNER)

    str.map { key =>
      cacheService.getString(Constants.CACHE_KEY_TOKEN_PREFIX + key) match {
        case Some(value) => {
          val vc = value.split(Constants.SPLIT_CHAR).toVector
          val token = OwnerToken(vc(0).toLong, vecGet(vc, 1), vecGet(vc, 2), vecGet(vc, 3))
          Right(token)
        }
        case None =>
          Left(key)
      }
    }
  }

  @inline
  private def vecGet(vc: Vector[String], idx: Int): Option[String] = {
    if (vc.length > idx) Some(vc(idx)) else None
  }

  def createCookie(name: String, value: String): Cookie = {
    Cookie(name, value, Some(Constants.COOKIE_MAX_AGE), httpOnly = true)
  }

  def createSession(ownerToken: OwnerToken) = {
    val key = ownerToken.ownerId.toString + Constants.SPLIT_CHAR +
      DigestUtils.sha1Hex(Random.nextInt() + Random.nextString(12) + (System.currentTimeMillis() / 1000))

    // XXX 设置缓存最长时间为一天，登录Session状态满一天后强制退出
    cacheService.set(Constants.CACHE_KEY_TOKEN_PREFIX + key, ownerToken.toValue, Duration(1, TimeUnit.DAYS))

    createCookie(Constants.PS_TOKEN_OWNER, key)
  }

  def discardingSession(key: String = null): Cookie = {
    if (key == null) {
      cacheService.remove(key)
    }
    DiscardingCookie(Constants.PS_TOKEN_OWNER).toCookie
  }

}
