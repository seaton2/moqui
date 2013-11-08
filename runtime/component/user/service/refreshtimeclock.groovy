import java.sql.Timestamp


Timestamp ts = new Timestamp(0)
timeInConverted = ts.valueOf(timeIn)
timeOutConverted = ts.valueOf(timeOut)

Long duration =timeOutConverted.getTime()-timeInConverted.getTime()

int hours = duration/3600000
int minutes = (duration-(3600000*hours))/60000
hoursWorked= String.format("%02d:%02d",hours,minutes)
ec.service.sync().name("update#moqui.basic.Timeclock")
        .parameters([autoInc:autoInc, timeIn:timeIn, timeOut:timeOut, hoursWorked:hoursWorked]).call()
if (ec.message.errors) return