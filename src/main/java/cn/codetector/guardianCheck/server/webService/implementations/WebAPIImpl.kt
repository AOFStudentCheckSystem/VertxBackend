/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.webService.implementations

import cn.codetector.guardianCheck.server.data.students.StudentManager
import cn.codetector.guardianCheck.server.data.user.UserHash
import cn.codetector.guardianCheck.server.data.user.UserManager
import cn.codetector.guardianCheck.server.data.user.WebUser
import cn.codetector.guardianCheck.server.mail.MailService
import cn.codetector.guardianCheck.server.mail.MailTemplate
import cn.codetector.guardianCheck.server.webService.IWebAPIImpl
import cn.codetector.guardianCheck.server.webService.WebAPIImpl
import cn.codetector.util.Validator.MD5
import com.google.common.io.ByteStreams
import com.google.common.io.CharStreams
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router
import net.coobird.thumbnailator.Thumbnails
import java.io.*
import java.util.*

/**
 * Created by codetector on 21/11/2016.
 */
@WebAPIImpl(prefix = "web")
class WebAPIImpl : IWebAPIImpl {
    private val logger = LoggerFactory.getLogger("WebPortal API")
    private val noAuthExceptions: Set<String> = hashSetOf("/web/auth", "/web/register")
    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {

        val defaultMail = MailTemplate(CharStreams.toString(InputStreamReader(APIImplV2::class.java.getResourceAsStream("/webEmail.html"))))


        router.route().failureHandler { ctx ->
            ctx.response().setStatusCode(ctx.statusCode()).putHeader("Access-Control-Allow-Origin", "*").end()
        }
        //Pre-flight handler
        router.options().handler { ctx ->
            ctx.response().putHeader("Access-Control-Allow-Origin", "*")
            ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            ctx.response().putHeader("Access-Control-Allow-Headers", "Authorization")
            ctx.response().end()
        }
        //General Handlers
        router.route().handler { ctx ->
            var path = ctx.request().path()
            if (path.endsWith("/")) {
                path = path.substring(0, path.length - 1)
            }
            if (noAuthExceptions.contains(path)) {
                ctx.next()
            } else {
                val auth = ctx.request().getHeader("Authorization")
                if (UserHash.isAuthKeyValid(auth)) {
                    ctx.setUser(UserHash.getUserByAuthKey(auth))
                    ctx.next()
                } else {
                    ctx.fail(401)
                }
            }
        }
        router.route().handler { ctx ->
            ctx.response().putHeader("Content-Type", "text/json; charset=utf-8")
            ctx.response().putHeader("Access-Control-Allow-Origin", "*")
            ctx.next()
        }
        router.post().handler { ctx ->
            ctx.request().isExpectMultipart = true
            ctx.request().endHandler { aVoid -> ctx.next() }
        }

        //Login auth handler
        router.post("/auth").handler { ctx ->
            if (UserManager.hasUser(ctx.request().getFormAttribute("username"))) {
                val user = UserManager.getUserByUsername(ctx.request().getFormAttribute("username"))
                if (user.authenticate(ctx.request().getFormAttribute("password"))) {
                    val hash = UserHash.createWebUser(user)
                    ctx.response().end(JsonObject().put("token", hash).toString())
                } else {
                    ctx.fail(401)
                }
            } else {
                ctx.fail(401)
            }
        }
        router.post("/auth/verify").handler { ctx ->
            ctx.response().end(ctx.user().principal().toString())
        }
        //User Handlers
        router.get("/user/permissions").handler { ctx ->
            ctx.response().end(JsonObject().put("permissions", JsonArray((ctx.user() as WebUser).user.role.allPermissions())).toString())
        }

        //Student Handlers
        //All Student handler
        router.get("/student/all").handler { ctx ->
            ctx.user().isAuthorised("readStudent", { auth ->
                if (auth.result()) {
                    ctx.response().end(JsonObject().put("students", JsonArray(StudentManager.allStudentsAsJsonArray())).toString())
                } else {
                    ctx.fail(401)
                }
            })
        }
        router.post("/student/:stuId/update").handler { ctx ->
            ctx.user().isAuthorised("updateStudent", { auth ->
                if (auth.result()) {
                    val stuId = ctx.pathParam("stuId")
                    val rfid = ctx.request().getFormAttribute("RFID")
                    val student = StudentManager.findStudentById(stuId)
                    if (student != null) {
                        student.rfid = rfid
                        StudentManager.saveToDatabase()
                        ctx.response().end()
                    } else {
                        ctx.response().setStatusCode(401).end("No student with ID $stuId found")
                    }
                } else {
                    ctx.fail(401)
                }
            })
        }

        router.get("/student/:stuID/image/:size").blockingHandler { ctx ->
            ctx.user().isAuthorised("readImage") { res ->
                if (res.result()) {
                    try {
                        val stuId = ctx.pathParam("stuID")
                        val size = Integer.parseInt(ctx.pathParam("size"))
                        val imageFile = File("./pics/$stuId.jpg")
                        if (imageFile.exists() && imageFile.isFile) {
                            val byteStream = ByteArrayOutputStream()
                            Thumbnails.of(imageFile).size(size, Math.round(size * 1.5).toInt()).toOutputStream(byteStream)
                            ctx.response().putHeader("Content-Type", "image/jpeg")
                            val bytes = byteStream.toByteArray()
                            val buffer = Buffer.buffer(bytes)
                            ctx.response().putHeader("etag", MD5.getMD5String(bytes))
                            ctx.response().end(buffer)
                        } else {
                            ctx.fail(404)
                        }
                    } catch (e: Throwable) {
                        ctx.fail(500)
                    }
                } else {
                    ctx.fail(401)
                }
            }
        }

        router.get("/student/:stuID/image").blockingHandler { ctx ->
            ctx.user().isAuthorised("readImage") { res ->
                if (res.result()) {
                    val stuId = ctx.pathParam("stuID")
                    val magic = File("./pics/$stuId.jpg")
                    ctx.response().putHeader("Content-Type", "image/jpeg")
                    try {
                        val `is` = FileInputStream(magic)
                        val bytes = ByteStreams.toByteArray(`is`)
                        val buffer = Buffer.buffer(bytes)
                        ctx.response().putHeader("etag", MD5.getMD5String(bytes))
                        ctx.response().end(buffer)
                    } catch (e: IOException) {
                        ctx.fail(404)
                    }

                } else {
                    ctx.fail(401)
                }
            }
        }

        router.get("/event/list").handler { ctx ->
            ctx.user().isAuthorised("readEvent") { v ->
                if (v.result()) {
                    dbClient.getConnection { conn ->
                        if (conn.succeeded()) {
                            conn.result().query("SELECT `eventId`, `eventName`, UNIX_TIMESTAMP(`eventTime`) as `eventTime`, `eventStatus` FROM `dummyevent` ORDER BY `eventTime` DESC") { res ->
                                if (res.succeeded()) {
                                    ctx.response().end(JsonObject().put("events", res.result().rows).toString())
                                } else {
                                    ctx.fail(res.cause())
                                }
                                conn.result().close()
                            }
                        } else {
                            ctx.fail(conn.cause())
                        }
                    }
                } else {
                    ctx.fail(401)
                }
            }
        }

        router.get("/api/event/:eventId/detail").handler { ctx ->
            ctx.user().isAuthorised("readEvent") { v ->
                if (v.result()) {
                    dbClient.getConnection { conn ->
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT `studentID` as `studentId` ,`checkinTime`, NULL as `checkoutTime` FROM `dummycheck` WHERE `event` = ?",
                                    JsonArray().add(ctx.pathParam("eventId"))) { res ->
                                if (res.succeeded()) {
                                    ctx.response().end(JsonObject().put("students", res.result().rows).toString())
                                } else {
                                    ctx.fail(res.cause())
                                }
                                conn.result().close()
                            }
                        } else {
                            ctx.fail(conn.cause())
                        }
                    }
                } else {
                    ctx.fail(401)
                }
            }
        }

        router.post("/event/:eventId/send").handler { ctx ->
            ctx.user().isAuthorised("sendEmail") { v ->
                if (v.result()) {
                    dbClient.getConnection { conn ->
                        val eventId = ctx.pathParam("eventId")
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT `eventName`, `eventStatus` FROM `dummyevent` WHERE `eventId`=? AND `eventStatus` > 0", JsonArray().add(eventId)) { res ->
                                if (res.succeeded()) {
                                    if (res.result().numRows == 1) {
                                        val eventStatus = res.result().rows[0].getInteger("eventStatus")
                                        val extraRecipientsStr = ctx.request().getFormAttribute("recipients")
                                        var extraRecipients: JsonArray? = null
                                        if (extraRecipientsStr != null) {
                                            extraRecipients = JsonObject(extraRecipientsStr).getJsonArray("recipients")
                                            sendMail(MailTemplate(defaultMail), dbClient, eventId, eventStatus, res.result().rows[0].getString("eventName"), extraRecipients, {
                                                if (it.result()) {
                                                    ctx.response().end()
                                                } else {
                                                    ctx.fail(500)
                                                }
                                            })
                                        } else {
                                            ctx.fail(400)
                                        }
                                    } else {
                                        ctx.fail(500)
                                    }
                                } else {
                                    ctx.fail(res.cause())
                                }
                                conn.result().close()
                            }
                        } else {
                            ctx.fail(conn.cause())
                        }
                    }
                } else {
                    ctx.fail(v.cause())
                }
            }
        }
    }

    private fun sendMail(mailTemplate: MailTemplate, client: JDBCClient, formId: String, formStatus: Int, formName: String, extraRecipients: JsonArray?, result: (AsyncResult<Boolean>) -> Unit) {
        client.getConnection { sqlConnectionAsyncResult ->
            if (sqlConnectionAsyncResult.succeeded()) {
                sqlConnectionAsyncResult.result().queryWithParams("SELECT (select lastName from dummystudent where dummycheck.studentID = dummystudent.studentID) as lastName, " +
                        "(select firstName from dummystudent where dummycheck.studentID = dummystudent.studentID) as firstName, " +
                        "(select nickName from dummystudent where dummycheck.studentID = dummystudent.studentID) as nickName, " +
                        "(select dorm from dummystudent where dummycheck.studentID = dummystudent.studentID) as dorm, " +
                        "`checkinTime` ,`checkoutTime` FROM `dummycheck` WHERE `event`=? ORDER BY `lastName` ASC", JsonArray().add(formId)) { res ->
                    if (res.succeeded()) {
                        mailTemplate.set("eventName", formName)
                        mailTemplate.set("eventId", formId)
                        mailTemplate.set("status", formStatus)
                        val lines = ArrayList<String>()
                        res.result().rows.forEach { row ->
                            val sb = StringBuilder()
                                    .append(row.getString("lastName"))
                                    .append(", ")
                                    .append(row.getString("nickName"))
                                    .append("-")
                                    .append(row.getString("dorm", "Day"))
                            lines.add(sb.toString())
                        }
                        mailTemplate.set("count", lines.size.toString())
                        mailTemplate.setTitle(formName)
                        mailTemplate.setList("studentList", lines)
                        extraRecipients?.forEach { o -> mailTemplate.addRecipient(o.toString()) }
                        MailService.sendMessage(MailService.getEmail(mailTemplate))
                        result.invoke(Future.succeededFuture(true))
                    } else {
                        logger.error(res.cause())
                        result.invoke(Future.succeededFuture(false))
                    }
                    sqlConnectionAsyncResult.result().close()
                }
            } else {
                logger.error(sqlConnectionAsyncResult.cause())
                result.invoke(Future.succeededFuture(false))
            }
        }
    }
}