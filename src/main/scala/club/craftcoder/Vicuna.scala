package club.craftcoder

import club.craftcoder.vicuna.core.{StatusDef, TransferDef}
import com.ecfront.common.Resp

object Vicuna {

    def define(flowCode:String,flowName:String,status:List[StatusDef],transition:List[TransferDef]):Resp[Void]={
    Resp.success()
    }

  def start[E](flowCode:String,objCode:String,objName:String,obj:E):Resp[Void]={

  }

  def next(flowCode:String):Resp[Void]={
    Resp.success()
  }

}
