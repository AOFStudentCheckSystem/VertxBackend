package cn.codetector.guardianCheck.server.webService.implementations;

import cn.codetector.guardianCheck.server.webService.IWebAPIImpl;
import cn.codetector.guardianCheck.server.webService.WebAPIImpl;
import cn.codetector.util.Validator.MD5;
import cn.codetector.util.Validator.SHA;
import com.google.common.io.ByteStreams;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@WebAPIImpl(prefix = "v2")
public class APIImplV2 implements IWebAPIImpl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void initAPI(@NotNull Router router, @NotNull Vertx sharedVertx, @NotNull JDBCClient dbClient) {
        JWTAuth jwtAuth = JWTAuth.create(sharedVertx, new JsonObject().put("keyStore", new JsonObject()
                .put("path", "keystore.jceks")
                .put("type", "jceks")
                .put("password", "secret")));
        JWTAuthHandler authHandler = JWTAuthHandler.create(jwtAuth, "/v2/api/auth");
        router.options("/api/*").handler(ctx -> {
            ctx.response().putHeader("Access-Control-Allow-Origin", "*");
            ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST OPTIONS");
            ctx.response().putHeader("Access-Control-Allow-Headers", "Authorization");
            ctx.response().end();
        });
        router.route().handler(ctx -> {
            ctx.response().putHeader("Content-Type", "text/json; charset=utf-8");
            ctx.next();
        });
        router.route("/api/*").handler(ctx -> {
            ctx.response().putHeader("Access-Control-Allow-Origin", "*");
            ctx.next();
        });
        router.route("/api/*").handler(authHandler);
        router.post("/api/*").handler(ctx -> {
            ctx.request().setExpectMultipart(true);
            ctx.request().endHandler(aVoid -> {
                ctx.next();
            });
        });


        // Authentication
        // {permissions:["readEvent","updateEvent","readStudent","sendEmail","readImage"]}
        // {permissions:["readEvent","addEvent","removeEvent","updateEvent","readStudent","addStudent","updateStudent","sendEmail","readImage"]}
        router.post("/api/auth").handler(ctx -> {
            dbClient.getConnection(conn -> {
                if (conn.succeeded()) {
                    String username = ctx.request().getFormAttribute("username");
                    String pw = SHA.getSHA256String(ctx.request().getFormAttribute("password"));
                    conn.result().queryWithParams("SELECT (SELECT `permissions` FROM `dummypermission` WHERE `dummyauth`.`role`=`dummypermission`.`role`) as `permissions`, `password` FROM `dummyauth` WHERE `username`=?", new JsonArray().add(username), res -> {
                        if (res.succeeded()) {
                            if (res.result().getNumRows() > 0 && res.result().getRows().get(0).getString("password").equals(pw)) {
                                try {
                                    JsonArray permissionArray = new JsonObject(res.result().getRows().get(0).getString("permissions")).getJsonArray("permissions");
                                    JWTOptions options = new JWTOptions();
                                    permissionArray.forEach(obj -> {
                                        options.addPermission(String.valueOf(obj));
                                    });
                                    ctx.response().end(new JsonObject().put("token", jwtAuth.generateToken(new JsonObject()
                                            .put("user", username), options)).toString());
                                } catch (Exception e) {
                                    ctx.fail(e);
                                }
                            } else {
//                                logger.warn("host: " + ctx.request().host() + " username: " + username);
                                ctx.fail(401);
                            }
                        } else {
                            ctx.fail(res.cause());
                        }
                        conn.result().close();
                    });
                } else {
                    ctx.fail(conn.cause());
                }
            });
        });
        //Deprecation
        router.get("/api/student/list").handler(ctx -> {
            ctx.user().isAuthorised("readStudent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().query("SELECT `studentID` as `id`,`firstName` as `fn`,`lastName` as `ln`,`nickName` as `nn` , `rfid` is not null as `exist`, `dorm`, `grade` FROM `dummystudent`", res -> {
                                if (res.succeeded()) {
                                    ctx.response().end(new JsonObject().put("students", res.result().getRows()).toString());
                                } else {
                                    ctx.fail(res.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(v.cause());
                }
            });
        });
        router.get("/api/student/all").handler(ctx -> {
            ctx.user().isAuthorised("readStudent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().query("SELECT `studentID` as `studentId`, `firstName`, `lastName`, `nickName`, `rfid`, `grade`, `type`, `dorm` FROM `dummystudent` WHERE 1", cbk -> {
                                if (cbk.succeeded()) {
                                    ctx.response().end(new JsonObject().put("students", cbk.result().getRows()).toString());
                                } else {
                                    ctx.fail(cbk.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        // Deprecation
        router.get("/api/student/count").handler(ctx -> {
            ctx.user().isAuthorised("readStudent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().query("SELECT count(*) FROM `dummystudent`", res -> {
                                if (res.succeeded()) {
                                    final int total = res.result().getResults().get(0).getInteger(0);
                                    conn.result().query("SELECT count(*) FROM `dummystudent` WHERE `rfid` IS NOT NULL", res2 -> {
                                        if (res2.succeeded()) {
                                            int done = res2.result().getResults().get(0).getInteger(0);
                                            ctx.response().end(new JsonObject().put("total", total).put("done", done).toString());
                                        } else {
                                            ctx.fail(res2.cause());
                                        }
                                        conn.result().close();
                                    });
                                } else {
                                    ctx.fail(res.cause());
                                    conn.result().close();
                                }
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/student/:stuId/update").handler(ctx -> {
            ctx.user().isAuthorised("updateStudent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            String studentID = ctx.pathParam("stuId");
                            String rfid = ctx.request().getFormAttribute("RFID");
                            conn.result().updateWithParams("INSERT INTO `dummystudent` (`studentID`,`rfid`) VALUES (?,?) ON DUPLICATE KEY UPDATE `rfid` = VALUES(rfid);",
                                    new JsonArray().add(studentID).add(rfid), cbk -> {
                                        if (cbk.succeeded()) {
                                            ctx.response().end();
//                                            SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_STUDENT, studentID));
                                        } else {
                                            ctx.fail(cbk.cause());
                                        }
                                        conn.result().close();
                                    });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.get("/api/student/:stuID/image").blockingHandler(ctx -> {
            ctx.user().isAuthorised("readImage", res -> {
                if (res.result()) {
                    String stuId = ctx.pathParam("stuID");
                    File magic = new File("./pics/" + stuId + ".jpg");
                    ctx.response().putHeader("Content-Type", "image/jpeg");
                    try {
                        InputStream is = new FileInputStream(magic);
                        byte[] bytes = ByteStreams.toByteArray(is);
                        Buffer buffer = Buffer.buffer(bytes);
                        ctx.response().putHeader("etag", MD5.getMD5String(bytes));
                        ctx.response().end(buffer);
                    } catch (IOException e) {
                        ctx.fail(404);
                    }
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.get("/api/event/list").handler(ctx -> {
            ctx.user().isAuthorised("readEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().query("SELECT `eventId`, `eventName`, UNIX_TIMESTAMP(`eventTime`) as `eventTime`, `eventStatus` FROM `dummyevent` ORDER BY `eventTime` DESC", res -> {
                                if (res.succeeded()) {
                                    ctx.response().end(new JsonObject().put("events", res.result().getRows()).toString());
                                } else {
                                    ctx.fail(res.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/event/add").handler(ctx -> {
            ctx.user().isAuthorised("addEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            String eventId = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
                            String eventName = ctx.request().getFormAttribute("eventName");
                            conn.result().updateWithParams("INSERT INTO `dummyevent` (`eventId`, `eventName`, `eventStatus`) VALUES (?, ?, ?)",
                                    new JsonArray().add(eventId).add(eventName).add(0), res -> {
                                        if (res.succeeded()) {
//                                            SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENTS_LIST, "ADD"));
                                            ctx.response().end(new JsonObject().put("eventId", eventId).toString());
                                        } else {
                                            ctx.fail(res.cause());
                                        }
                                        conn.result().close();
                                    });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.get("/api/event/:eventId/detail").handler(ctx -> {
            ctx.user().isAuthorised("readEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT `studentID` as `studentId` ,`checkinTime`, NULL as `checkoutTime` FROM `dummycheck` WHERE `event` = ?",
                                    new JsonArray().add(ctx.pathParam("eventId")), res -> {
                                        if (res.succeeded()) {
                                            ctx.response().end(new JsonObject().put("students", res.result().getRows()).toString());
                                        } else {
                                            ctx.fail(res.cause());
                                        }
                                        conn.result().close();
                                    });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.get("/api/event/:eventId/detail/mobile").handler(ctx -> {
            ctx.user().isAuthorised("readEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT `studentID`,`checkinTime`,`checkoutTime` FROM `dummycheck` WHERE `event` = ?",
                                    new JsonArray().add(ctx.pathParam("eventId")), res -> {
                                        if (res.succeeded()) {
                                            ctx.response().end(new JsonObject().put("students", res.result().getRows()).toString());
                                        } else {
                                            ctx.fail(res.cause());
                                        }
                                        conn.result().close();
                                    });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/event/:eventId/complete").handler(ctx -> {
            ctx.user().isAuthorised("updateEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            String eventId = ctx.pathParam("eventId");
                            conn.result().updateWithParams("UPDATE `dummyevent` SET `eventStatus`=? WHERE `eventId`=?",
                                    new JsonArray().add(2).add(eventId), res -> {
                                        if (res.succeeded()) {
//                                            SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENTS_LIST, "UPDATE"));
                                            ctx.response().end();
                                        } else {
                                            ctx.fail(res.cause());
                                        }
                                        conn.result().close();
                                    });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/event/:eventId/send").handler(ctx -> {
            ctx.user().isAuthorised("sendEmail", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        String eventId = ctx.pathParam("eventId");
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT `eventName` FROM `dummyevent` WHERE `eventId`=? AND `eventStatus`>1", new JsonArray().add(eventId), res -> {
                                if (res.succeeded()) {
                                    if (res.result().getNumRows() > 0) {
                                        String extraRecipientsStr = ctx.request().getFormAttribute("recipients");
//                                        JsonArray extraRecipients = null;
                                        if (extraRecipientsStr != null) {
//                                            extraRecipients = new JsonObject(extraRecipientsStr).getJsonArray("recipients");
//                                            sendMail(mailClient, new MailTemplate(defaultMail), dbClient, eventId, res.result().getRows().get(0).getString("eventName"), extraRecipients);
                                            ctx.response().end();
                                        } else {
                                            ctx.fail(400);
                                        }
                                    }
                                    ctx.fail(500);
                                } else {
                                    ctx.fail(res.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(v.cause());
                }
            });
        });
        router.post("/api/event/:eventId/delete").handler(ctx -> {
            ctx.user().isAuthorised("removeEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        String eventId = ctx.pathParam("eventId");
                        boolean force = Boolean.valueOf(ctx.request().getFormAttribute("force"));
                        if (conn.succeeded()) {
                            conn.result().updateWithParams("DELETE FROM `dummyevent` WHERE `eventId`=? AND `eventStatus`<?", new JsonArray().add(eventId).add(force ? 3 : 1), res -> {
                                if (res.succeeded()) {
//                                    SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENTS_LIST, "DELETE"));
                                    ctx.response().end();
                                } else {
                                    ctx.fail(res.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(v.cause());
                }
            });
        });
        router.post("/api/event/:eventId/update").handler(ctx -> {
            ctx.user().isAuthorised("updateEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            String eventId = ctx.pathParam("eventId");
//                            SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENT_CONTENT, eventId));
                            conn.result().queryWithParams("SELECT `eventStatus` FROM `dummyevent` WHERE `eventId`=?", new JsonArray().add(eventId), res -> {
                                if (res.succeeded()) {
                                    ctx.response().end();
                                    if (res.result().getNumRows() > 0) {
                                        int eventStatus = res.result().getRows().get(0).getInteger("eventStatus");
                                        if (eventStatus < 2) {
                                            int status = 1;
                                            if (Boolean.valueOf(ctx.request().getFormAttribute("completed"))) {
                                                status = 2;
                                            }
                                            if (eventStatus != status) {
                                                int finalStatus = status;
                                                dbClient.getConnection(co -> {
                                                    if (co.succeeded()) {
                                                        co.result().updateWithParams("UPDATE `dummyevent` SET `eventStatus`=? WHERE `eventId`=?", new JsonArray().add(finalStatus).add(eventId), result -> {
                                                            if (!result.succeeded()) {
//                                                                ctx.response().end();
                                                                ctx.fail(result.cause());
                                                            }
                                                            co.result().close();
                                                        });
                                                    } else {
                                                        ctx.fail(co.cause());
                                                    }
                                                });
                                            }
                                            String dataStr = ctx.request().getFormAttribute("data");
                                            if (dataStr != null) {
                                                JsonObject data = new JsonObject(dataStr);
                                                // not required
                                                JsonArray remove = data.getJsonArray("remove");
                                                // not required
                                                JsonArray add = data.getJsonArray("add");
                                                if (add != null) {
                                                    List<JsonArray> addList = new ArrayList<>();
                                                    add.forEach(o -> {
                                                        if (o instanceof JsonObject) {
                                                            JsonObject stuObj = ((JsonObject) o);
                                                            String stuId = stuObj.getString("id");
                                                            String inTime = stuObj.getString("checkin");
                                                            String outTime = stuObj.getString("checkout");
                                                            addList.add(new JsonArray().add(stuId).add(inTime).add(outTime == null ? "" : outTime).add(eventId));
                                                        }
                                                    });
                                                    dbClient.getConnection(co -> {
                                                        if (co.succeeded()) {
                                                            co.result().batchWithParams("INSERT INTO `dummycheck` (`studentID`, `checkinTime`, `checkoutTime`, `event`) VALUES (?, ?, ?, ?)", addList, result -> {
                                                                if (!result.succeeded()) {
//                                                                    ctx.response().end();
                                                                    ctx.fail(result.cause());
                                                                }
                                                                co.result().close();
                                                            });
                                                        } else {
                                                            ctx.fail(co.cause());
                                                        }
                                                    });
                                                }
                                                if (remove != null) {
                                                    List<JsonArray> removeList = new ArrayList<>();
                                                    remove.forEach(o -> {
                                                        removeList.add(new JsonArray().add(o).add(eventId));
                                                    });
                                                    dbClient.getConnection(co -> {
                                                        if (co.succeeded()) {
                                                            co.result().batchWithParams("DELETE FROM `dummycheck` WHERE `studentID`=? AND `event`=?", removeList, result -> {
                                                                if (!result.succeeded()) {
//                                                                    ctx.response().end();
                                                                    ctx.fail(result.cause());
                                                                }
                                                                co.result().close();
                                                            });
                                                        } else {
                                                            ctx.fail(co.cause());
                                                        }
                                                    });
                                                }
                                            }
                                        } else {
                                            ctx.fail(401);
                                        }
                                    }
                                } else {
                                    ctx.fail(res.cause());
                                }
                                conn.result().close();
                            });
                        } else {
                            ctx.fail(conn.cause());
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/event/:eventId/add").handler(ctx -> {
            verifyUpdateEvent(dbClient, ctx, result -> {
                if (result.result()) {
                    String eventId = ctx.pathParam("eventId");
                    String dataStr = ctx.request().getFormAttribute("data");
                    logger.trace("Add Request TargetEvent:"+eventId +" DATA:"+dataStr);
                    if (dataStr != null) {
                        JsonObject data = new JsonObject(dataStr);
                        // not required
                        JsonArray add = data.getJsonArray("add");
                        JsonArray addDuplicate = new JsonArray();
                        if (add != null) {
                            List<JsonArray> addList = new ArrayList<>();
                            add.forEach(o -> {
                                if (o instanceof JsonObject) {
                                    JsonObject stuObj = ((JsonObject) o);
                                    String stuId = stuObj.getString("id");
                                    String inTime;
                                    String outTime;
                                    if (stuObj.getValue("checkout") instanceof String){
                                        outTime = stuObj.getString("checkout");
                                    }else{
                                        outTime = String.valueOf(stuObj.getLong("checkout"));
                                    }
                                    if (stuObj.getValue("checkin") instanceof String){
                                        inTime = stuObj.getString("checkin");
                                    }else{
                                        inTime = String.valueOf(stuObj.getLong("checkin"));
                                    }
                                    addList.add(new JsonArray()
                                            .add(stuId)
                                            .add(inTime)
                                            .add(outTime == null ? "" : outTime)
                                            .add(eventId));
                                    addDuplicate.add(new JsonObject(stuObj.toString()).put("event", eventId));
                                }
                            });
                            dbClient.getConnection(co -> {
                                if (co.succeeded()) {
                                    co.result().batchWithParams("INSERT INTO `dummycheck` (`studentID`, `checkinTime`, `checkoutTime`, `event`) VALUES (?, ?, ?, ?)", addList, res -> {
                                        if (res.succeeded()) {
//                                            SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENT_CONTENT_ADD,new JsonObject().put("add_students",addDuplicate).toString()));
                                            ctx.response().end();
                                        } else {
                                            ctx.fail(res.cause());
                                        }
                                        co.result().close();
                                    });
                                } else {
                                    ctx.fail(co.cause());
                                }
                            });
                        } else {
                            ctx.response().end();
                        }
                    } else {
                        ctx.response().end();
                    }
                } else {
                    ctx.fail(result.cause());
                }
            });
        });
        router.post("/api/event/:eventId/remove").handler(ctx -> {
            verifyUpdateEvent(dbClient, ctx, result -> {
                String eventId = ctx.pathParam("eventId");
                String dataStr = ctx.request().getFormAttribute("data");
                if (dataStr != null) {
                    JsonObject data = new JsonObject(dataStr);
                    // not required
                    JsonArray remove = data.getJsonArray("remove");
                    if (remove != null) {
                        List<JsonArray> removeList = new ArrayList<>();
                        remove.forEach(o -> {
                            removeList.add(new JsonArray().add(o).add(eventId));
                        });
                        dbClient.getConnection(co -> {
                            if (co.succeeded()) {
                                co.result().batchWithParams("DELETE FROM `dummycheck` WHERE `studentID`=? AND `event`=?", removeList, res -> {
                                    if (res.succeeded()) {
//                                        SocketManager.getSharedSocketManager().notifyAllClient(new NetNotificationPacket(Notification.UPDATE_EVENT_CONTENT_DEL,new JsonObject().put("remove_students",remove).toString()));
                                        ctx.response().end();
                                    } else {
                                        ctx.fail(res.cause());
                                    }
                                    co.result().close();
                                });
                            } else {
                                ctx.fail(co.cause());
                            }
                        });
                    } else {
                        ctx.response().end();
                    }
                } else {
                    ctx.response().end();
                }
            });
        });
        router.exceptionHandler(e -> {
            if (e instanceof RuntimeException) {
                e.printStackTrace();
            }
            logger.error(e);
        });
    }

    private void verifyUpdateEvent(JDBCClient dbClient, RoutingContext ctx, Handler<AsyncResult<Boolean>> handler) {
        ctx.user().isAuthorised("updateEvent", v -> {
            if (v.result()) {
                dbClient.getConnection(conn -> {
                    if (conn.succeeded()) {
                        String eventId = ctx.pathParam("eventId");
                        conn.result().queryWithParams("SELECT `eventStatus` FROM `dummyevent` WHERE `eventId`=?", new JsonArray().add(eventId), res -> {
                            if (res.succeeded() && res.result().getNumRows() > 0) {
                                int eventStatus = res.result().getRows().get(0).getInteger("eventStatus");
                                if (eventStatus < 2) {
                                    if (eventStatus < 1) {
                                        dbClient.getConnection(co -> {
                                            if (co.succeeded()) {
                                                co.result().updateWithParams("UPDATE `dummyevent` SET `eventStatus`=? WHERE `eventId`=?", new JsonArray().add(1).add(eventId), result -> {
                                                    if (result.succeeded()) {
                                                        handler.handle(Future.succeededFuture(true));
                                                    } else {
                                                        handler.handle(Future.failedFuture(result.cause()));
                                                    }
                                                    co.result().close();
                                                });
                                            } else {
                                                handler.handle(Future.failedFuture(co.cause()));
                                            }
                                        });
                                    } else {
                                        handler.handle(Future.succeededFuture(true));
                                    }
                                } else {
                                    handler.handle(Future.failedFuture("Event Complete"));
                                }
                            } else {
                                handler.handle(Future.failedFuture(res.cause()));
                            }
                            conn.result().close();
                        });
                    } else {
                        handler.handle(Future.failedFuture(conn.cause()));
                    }
                });
            } else {
                handler.handle(Future.failedFuture(v.cause()));
            }
        });
    }
}
