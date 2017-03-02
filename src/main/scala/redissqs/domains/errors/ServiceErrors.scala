package redissqs.domains.errors

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}

package object ServiceErrors {

  sealed trait NotFoundError
  sealed trait InternalServerError

  def RedisDatabaseError  = new Errors(List(CommonError(name = "1001"))) with InternalServerError
  def RegisterQueuesError = new Errors(List(CommonError(name = "1002"))) with InternalServerError
  def Insert2QueueError   = new Errors(List(CommonError(name = "1003"))) with InternalServerError
  def Insert2ZsetError    = new Errors(List(CommonError(name = "1004"))) with InternalServerError
  def GetQueueNamesError  = new Errors(List(CommonError(name = "1005"))) with InternalServerError
  def DeBufError          = new Errors(List(CommonError(name = "1006"))) with InternalServerError

  def DataInZsetNotFound = new Errors(List(CommonError(name = "2001"))) with NotFoundError
  def QueueNotFound      = new Errors(List(CommonError(name = "2002"))) with NotFoundError

  def GetExpiredDataError            = new Errors(List(CommonError(name = "3001"))) with InternalServerError
  def ResetExpiredDataError          = new Errors(List(CommonError(name = "3002"))) with InternalServerError
  def RemoveExpiredDataFromZsetError = new Errors(List(CommonError(name = "3003"))) with InternalServerError
}
