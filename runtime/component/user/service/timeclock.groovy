

TimeClock = ec.entity.makeFind("moqui.basic.Timeclock").condition(ec.entity.conditionFactory.makeActionCondition("userId", "equals", "userId", "${ec.user.getUserId()}", null, false, false,false)).condition(ec.entity.conditionFactory.makeCondition([ec.entity.conditionFactory.makeActionCondition("status", "equals", "status", "TC_new", null, false, false, false), ec.entity.conditionFactory.makeActionCondition("status", "equals", "status", "TC_in", null, false, false, false)], org.moqui.impl.entity.EntityConditionFactoryImpl.getJoinOperator("or"))).one()

if (TimeClock.status=='TC_in') {
    status = ("TC_done")
    Long duration = ec.user.getNowTimestamp().getTime()-TimeClock.timeIn.getTime()

    int hours = duration/3600000
    int minutes = (duration-(3600000*hours))/60000
    finalCount= String.format("%02d:%02d",hours,minutes)





    if (true) {
        ec.service.sync().name("update#moqui.basic.Timeclock")
                .parameters([autoInc:TimeClock.autoInc, status:status, timeOut:ec.user.getNowTimestamp(), hoursWorked:finalCount]).call()
        if (ec.message.errors) return
    }
    status = ("TC_new")
    if (true) {
        ec.service.sync().name("create#moqui.basic.Timeclock")
                .parameters([userId:TimeClock.userId, status:status]).call()
        if (ec.message.errors) return
    }
    userIs = ("CLOCKED OUT")
    if (true) {
        ec.service.sync().name("update#moqui.security.UserAccount")
                .parameters([userId:TimeClock.userId, status:userIs]).call()
        if (ec.message.errors) return
    }
}
if (TimeClock.status=='TC_new') {
    status = ("TC_in")
    if (true) {
        ec.service.sync().name("update#moqui.basic.Timeclock")
                .parameters([autoInc:TimeClock.autoInc, status:status, timeIn:ec.user.getNowTimestamp()]).call()
        if (ec.message.errors) return
    }
    userIs = ("CLOCKED IN")
    if (true) {
        ec.service.sync().name("update#moqui.security.UserAccount")
                .parameters([userId:TimeClock.userId, status:userIs]).call()
        if (ec.message.errors) return
    }
}
