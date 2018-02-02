# NOTE: for this script to work:
#       - the ftpTestsDirectory needs to exist on ftp server home
#       - adjust the variables below
# NOTE: if performace decreases over time, take a look at ftp.nlst(), used
#       every time to check if the folder for the current day exists
#       (the more folders, the more time and bandwidth are consumed)

import mysql.connector
from datetime import date
from datetime import datetime
from datetime import timedelta
from ftplib import FTP
import os
import sys

# START PROPERITES ###############################

#debugEnabled = True
#ftpServer = "NASSRV"
#ftpUsername = "USER"
#ftpPassword = "user"
#ftpTestsDirectory = "DTDV"
#mysqlServer = "localhost"
#mysqlUser = "application"
#mysqlPassword = "12Application"
#mysqlDatabase = "tracking_system"
#machineA = "TRI1" # Name used on MySQL server
#machineB = "TRI2" # Name used on MySQL server

# TESTING PROPERTIES:
debugEnabled = True
ftpServer = "localhost"
ftpUsername = "abdhul"
ftpPassword = "12Vezzavide"
ftpTestsDirectory = "tests"
mysqlServer = "192.168.1.4"
mysqlUser = "application"
mysqlPassword = "12Application"
mysqlDatabase = "tracking_system"
machineA = "TRI1" # Name used on MySQL server
machineB = "TRI2" # Name used on MySQL servers

# END PROPERTIES #################################

def abortScript():
    debug("Aborting script")
    if debugEnabled:
        debug("Execution time: " + str(datetime.now() - start))
    dataConnection.close()
    exit()

def debug(message):
    if debugEnabled:
        print message

# Checks if segmentToCheck is contained in timelineWithSegments
def timelineContains(timelineWithSegments, segmentToCheck):
    for segment in timelineWithSegments:
        if (segmentToCheck[0] >= segment[0]) and (segmentToCheck[1] <= segment[1]):
            return True
    
    return False

# Returns a timedelta of tot work, considering overlapping segments
# This is the algorithm:
#   - put every start of segment and every end of segment (from both timelines)
#     inside a general timeline (wich is, at this point, a list of points
#     in time, and nothing more)
#   - sort the timeline, from oldest point to newest point
#   - iterating through the timeline, consider every point and its successor
#     as a time segment. If that segment is contained in at least one of the
#     two timelines, calculate its timedelta and add it to the total
#     count of work hours
def calculateWork(timelineA, timelineB):
    timeline = []
    totWork = timedelta()
    for segment in timelineA:
        timeline.append(segment[0])
        timeline.append(segment[1])
    for segment in timelineB:
        timeline.append(segment[0])
        timeline.append(segment[1])
    timeline.sort()
    # From here, timeline contains EACH point of each timeline, disregarding
    # if it's a start or an end of a segment, sorted
    for i in range(0, len(timeline)-1):
        segmentToCheck = [timeline[i], timeline[i+1]]
        if timelineContains(timelineA, segmentToCheck) or timelineContains(timelineB, segmentToCheck):
            segmentDelta = segmentToCheck[1] - segmentToCheck[0]
            totWork += segmentDelta
            debug("Added " + str(segmentDelta) + " to total count (segment from " + str(segmentToCheck[0]) + " to " + str(segmentToCheck[1]) + ")")
    
    return totWork
    
# END OF FUNCTIONS

# START OF MAIN LOGIC

# Mega-try-catch to log any problem
try:
    # If an argument exists, it is expected to be the desired date for querying
    if len(sys.argv) > 1:
        today = sys.argv[1]
    # otherwise, today is used
    else:
        today = str(date.today())
    
    debug("Executing script for " + today)
    
    start = datetime.now()
    
    # Connection to query data
    dataConnection = mysql.connector.connect(user = mysqlUser,
                                  password = mysqlPassword,
                                  host = mysqlServer,
                                  database = mysqlDatabase)

    userCursor = dataConnection.cursor()
    lastSentRecord = 0
    
    # Loads last sent record (saved on file)
    try:
        lastSentRecordFile = open("lastSentRecord.txt", "r")
        lastSentRecord = int(lastSentRecordFile.read())
        lastSentRecordFile.close()
    except Exception:
        debug("No lastSentRecord file")

#    # Gets list of users for today tests
#    userQuery = ("SELECT DISTINCT user.surname, user.name, user.username "
#                 "FROM tracking_system.test "
#                 "INNER JOIN user "
#                 "ON user.username = test.user "
#                 "WHERE DATE(test.datetime) = '" + today + "';")
    
    userQuery = ("SELECT DISTINCT user.surname, user.name, user.username "
                "FROM tracking_system.user "
                "INNER JOIN (SELECT login.username FROM test "
                            "INNER JOIN login "
                            "ON test.login_id = login.id "
                            "WHERE DATE(test.datetime) = '" + today + "') "
                "AS test_with_user "
                "ON user.username = test_with_user.username;")
    
    userCursor.execute(userQuery)
    # I need to fetch now the results so that row count can be incremented to its
    # exact value (otherwise it would stay at -1 until the whole cursor was iterated
    fetchedUserCursor = userCursor.fetchall()
    numberOfUsers = userCursor.rowcount

    # If there's no users for today, it means that there's no tests for now,
    # then do nothing
    if numberOfUsers == 0:
        debug("No users (i.e. no data) for today so far")
        abortScript()

    # Creates whole file for the current day
    fileName = today + ".csv"
    testCursor = dataConnection.cursor()
#    testQuery = ("SELECT test.id, "
#                    "test.datetime, "
#                    "test.board, "
#                    "test.good, "
#                    "login.machine, "
#                    "test.login_id, "
#                    "user.surname, "
#                    "user.name, "
#                    "user.username "
#                 "FROM tracking_system.test "
#                 "INNER JOIN user "
#                 "ON user.username = test.user "
#                 "WHERE DATE(test.datetime) = '" + today + "' "
#                 "AND test.id > '" + str(lastSentRecord) + "' "
#                 "ORDER BY test.id;")
    
    testQuery = ("SELECT test_with_login.id, "
                    "test_with_login.datetime, "
                    "test_with_login.board, "
                    "test_with_login.good, "
                    "test_with_login.machine, "
                    "test_with_login.login_id, "
                    "user.surname, "
                    "user.name, "
                    "user.username "
                "FROM tracking_system.user "
                "INNER JOIN (SELECT test.id, "
                                "test.datetime, "
                                "test.board, "
                                "test.good, "
                                "login.username, "
                                "login.machine, "
                                "login.id AS login_id FROM test "
                            "INNER JOIN login "
                            "ON test.login_id = login.id "
                            "WHERE DATE(test.datetime) = '" + today + "' "
                            "AND test.id > '" + str(lastSentRecord) + "') "
                "AS test_with_login "
                "ON user.username = test_with_login.username "
                "ORDER BY test_with_login.id;")
    
    testCursor.execute(testQuery)
    fetchedTestCursor = testCursor.fetchall()
    numberOfNewTests = testCursor.rowcount
    
    if numberOfNewTests == 0:
        debug("No new records, no need to update FTP server, quitting")
        abortScript()
    
    csvIn = open(fileName + ".temp", "w")
    csvIn.write("test_id;datetime;scheda;good;macchina;login_id;cognome;nome;username\r\n")
    for line in fetchedTestCursor:
        csvIn.write(str(line[0]).encode('utf-8')
        + ";" + str(line[1]).encode('utf-8')
        + ";" + line[2].encode('utf-8')
        + ";" + str(line[3]).encode('utf-8')
        + ";" + line[4].encode('utf-8')
        + ";" + str(line[5]).encode('utf-8')
        + ";" + line[6].encode('utf-8')
        + ";" + line[7].encode('utf-8')
        + ";" + line[8].encode('utf-8')
        + "\r\n")
        lastRecord = line[0]
    csvIn.close()
    testCursor.close()
    
    # Compares last sent record and last record just retrieved,
    # if they are equal, there's no need to upload data to the FTP server
    debug("Last record from db for today is " + str(lastRecord))
    debug("Last record sent to db is " + str(lastSentRecord))
    
    # If script reaches this point, it means that an update to the FTP
    # server is indeed necessary

    # Starts ftp connection, creates today's directory if necessary, enters it
    # and sends whole file for today
    ftp = FTP(ftpServer)
    ftp.login(ftpUsername, ftpPassword)
    ftp.cwd(ftpTestsDirectory)

    # Append data to whole FILE (whole db)
    wholeFile = "tracking_system_tests.csv"
    csvOut = open(fileName + ".temp", "r")
    if wholeFile in ftp.nlst():
        header = csvOut.readline()  # Skips header
    debug("Appending data to whole file for whole db")
    ftp.storbinary("APPE " + wholeFile, csvOut)
    csvOut.close()

    # NOTE: over time, this bit could take longer and longer: as the number
    # of folders increases, ftp.nlst() will need more time (and bandwidth)
    # to execute. If performance issues occur, try something new.
    if today not in ftp.nlst():
        ftp.mkd(today)
    ftp.cwd(today)
    csvOut = open(fileName + ".temp", "r")
    if fileName in ftp.nlst():
        header = csvOut.readline() # Skips header
    debug("Appending data to filw for today")
    ftp.storbinary("APPE " + fileName, csvOut)
    csvOut.close()
    os.remove(fileName + ".temp")
    lastSentRecordFile = open("lastSentRecord.txt", "w")
    lastSentRecordFile.write(str(lastRecord))
    lastSentRecordFile.close()
    debug(fileName + " sent to FTP server")

    # Then creates and send a file for each user of the current day
    for (surname, name, username) in fetchedUserCursor:
        debug("\n\nANALIZING NEW USER")
        userFileName = (surname.encode('utf-8') + "_"
                        + name.encode('utf-8') + "_("
                        + username.encode('utf-8') + ")_"
                        + today)
        testCursor = dataConnection.cursor()
#        testQuery = ("SELECT test.id, test.datetime, test.board, test.good, test.machine, test.login_id, user.surname, user.name, user.username "
#                     "FROM tracking_system.test "
#                     "INNER JOIN user "
#                     "ON user.username = test.user "
#                     "WHERE test.user = '" + username + "' "
#                     "AND DATE(test.datetime) = '" + today + "' "
#                     "AND test.id > '" + str(lastSentRecord) + "' "
#                     "ORDER BY test.id;")
        
        testQuery = ("SELECT test_with_login.id, "
                        "test_with_login.datetime, "
                        "test_with_login.board, "
                        "test_with_login.good, "
                        "test_with_login.machine, "
                        "test_with_login.login_id, "
                        "user.surname, "
                        "user.name, "
                        "user.username "
                    "FROM tracking_system.user "
                    "INNER JOIN (SELECT test.id, "
                                    "test.datetime, "
                                    "test.board, "
                                    "test.good, "
                                    "login.username, "
                                    "login.machine, "
                                    "login.id AS login_id FROM test "
                                "INNER JOIN login "
                                "ON test.login_id = login.id "
                                "WHERE DATE(test.datetime) = '" + today + "' "
                                "AND test.id > '" + str(lastSentRecord) + "') "
                    "AS test_with_login "
                    "ON user.username = test_with_login.username "
                    "WHERE test_with_login.username ='" + username + "' "
                    "ORDER BY test_with_login.id;")
        
        testCursor.execute(testQuery)
        csvIn = open(userFileName + ".csv" + ".temp", "w")
        csvIn.write("test_id;datetime;scheda;good;macchina;login_id;cognome;nome;username\r\n")
        for line in testCursor:
            csvIn.write(str(line[0]).encode('utf-8')
            + ";" + str(line[1]).encode('utf-8')
            + ";" + line[2].encode('utf-8')
            + ";" + str(line[3]).encode('utf-8')
            + ";" + line[4].encode('utf-8')
            + ";" + str(line[5]).encode('utf-8')
            + ";" + line[6].encode('utf-8')
            + ";" + line[7].encode('utf-8')
            + ";" + line[8].encode('utf-8')
            + "\r\n")
        csvIn.close()
        testCursor.close()
        # Creates folder for user (if it doesn't exist) and moves to it
        if userFileName not in ftp.nlst():
            ftp.mkd(userFileName)
        ftp.cwd(userFileName)
        csvOut = open(userFileName + ".csv" + ".temp", "r")
        if userFileName + ".csv" in ftp.nlst():
            header = csvOut.readline() # Skips header
        ftp.storbinary("APPE " + userFileName + ".csv" , csvOut)
        csvOut.close()
        os.remove(userFileName + ".csv" + ".temp")
        debug("File for " + surname.encode('utf-8') + " for " + today + " sent to FTP server")
        
        # Fetch working time segments for machineA
        workTimeCursor = dataConnection.cursor()
#        query = ("SELECT MIN(test.datetime), MAX(test.datetime) "
#                "FROM tracking_system.test "
#                "WHERE test.user = '" + username + "' "
#                "AND DATE(test.datetime) = '" + today + "' "
#                "AND test.machine = '" + machineA + "' "
#                "GROUP BY test.login_id;")
        
        query = ("SELECT MIN(test.datetime), MAX(test.datetime) "
                "FROM tracking_system.test "
                "INNER JOIN login "
                "ON test.login_id = login.id "
                "WHERE login.username = '" + username + "' "
                "AND DATE(test.datetime) = '" + today + "' "
                "AND login.machine = '" + machineA + "' "
                "GROUP BY test.login_id;")
        
        workTimeCursor.execute(query)
        workTimeMachineA = workTimeCursor.fetchall()
        
        # Fetch working time segments for machineB
        workTimeCursor = dataConnection.cursor()
#        query = ("SELECT MIN(test.datetime), MAX(test.datetime) "
#                "FROM tracking_system.test "
#                "WHERE test.user = '" + username + "' "
#                "AND DATE(test.datetime) = '" + today + "' "
#                "AND test.machine = '" + machineB + "' "
#                "GROUP BY test.login_id;")
        
        query = ("SELECT MIN(test.datetime), MAX(test.datetime) "
                "FROM tracking_system.test "
                "INNER JOIN login "
                "ON test.login_id = login.id "
                "WHERE login.username = '" + username + "' "
                "AND DATE(test.datetime) = '" + today + "' "
                "AND login.machine = '" + machineB + "' "
                "GROUP BY test.login_id;")
        
        workTimeCursor.execute(query)
        workTimeMachineB = workTimeCursor.fetchall()

        totalWork = timedelta()
        
        totTimeA = timedelta()
        for row in workTimeMachineA:
            # Adds 1 minute just in case this start and end of segment are the
            # same point in time (it happens if during a login_id only one
            # board test is logged (same to total work)
            delta = (row[1] - row[0]) + timedelta(minutes = 1)
            debug("lavoro su macchina A di " + str(delta))
            totTimeA += delta
            totalWork += timedelta(minutes = 1)
        debug("tot time for machine A and user " + username + ": " + str(totTimeA))
        
        totTimeB = timedelta()
        for row in workTimeMachineB:
            # Adds 1 minute just in case this start and end of segment are the
            # same point in time (it happens if during a login_id only one
            # board test is logged (same to total work)
            delta = (row[1] - row[0]) + timedelta(minutes = 1)
            debug("lavoro su macchina B di " + str(delta))
            totTimeB += delta
            totalWork += timedelta(minutes = 1)
        debug("tot time for machine B and user " + username + ": " + str(totTimeB))
        
        # Calculates total work adding each time segment (one for every
        # login_id) to the total, overlapping intersecting segments from
        # the two different timelines (machine A and B)
        totalWork += calculateWork(workTimeMachineA, workTimeMachineB)
        debug("Total work: " + str(totalWork))
        
        # Creates and sends total working hours file to FTP server
        totalWorkFileName = "TOT_ORE_" + userFileName + ".txt"
        totalWorkFile = open(totalWorkFileName + ".temp", "w")
        string = ("Data: " + today
                + "\r\nOperatore: " + surname.encode('utf-8')
                + " " + name.encode('utf-8')
                + "\r\n\r\nOre su macchina TRI1: " + str(totTimeA))
        # Prints sessions for A
        for row in workTimeMachineA:
            string += ("\r\n\t- sessione dalle "
                    + (row[0] - timedelta(minutes=1)).strftime('%H:%M:%S')
                    + " alle " + (row[1]).strftime('%H:%M:%S'))
        string += "\r\nOre su macchina TRI2: " + str(totTimeB)
        # Prints sessions for B
        for row in workTimeMachineB:
            string += ("\r\n\t- sessione dalle "
                    + (row[0] - timedelta(minutes=1)).strftime('%H:%M:%S')
                    + " alle " + (row[1]).strftime('%H:%M:%S'))
        string += ("\r\n\r\nTOTALE ORE (considerando eventuali sovrapposizioni):\r\n\t"
                + str(totalWork)
                + "\r\n\r\n(aggiornato automaticamente al "
                + datetime.now().strftime('%Y-%m-%d %H:%M:%S') + ")")
        totalWorkFile.write(string)
        totalWorkFile.close()
        totalWorkFile = open(totalWorkFileName + ".temp", "r")
        ftp.storbinary("STOR " + totalWorkFileName, totalWorkFile)
        totalWorkFile.close()
        os.remove(totalWorkFileName + ".temp")
        
        # Exits from user folder and moves to parent for next user
        ftp.cwd("../")
    # END USER FOR STATEMENT
    
    userCursor.close()
    
    dataConnection.close()
    ftp.quit()
    
    if debugEnabled:
        debug("Execution time: " + str(datetime.now() - start))

# Log exception with datetime, line number and exception message
except Exception as e:
    errorLog = open("error.log", "a")
    error = "[" + str(datetime.now()) + "], line " + str(sys.exc_info()[-1].tb_lineno) + ": " + str(e) + "\r\n\r\n"
    errorLog.write(error)
    debug(error)