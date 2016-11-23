package cn.codetector.guardianCheck.server.webService.implementations;

import cn.codetector.guardianCheck.server.data.user.User;
import cn.codetector.guardianCheck.server.data.user.UserHash;
import cn.codetector.guardianCheck.server.data.user.UserManager;
import cn.codetector.guardianCheck.server.webService.IWebAPIImpl;
import cn.codetector.guardianCheck.server.webService.WebAPIImpl;
import cn.codetector.guardianCheck.server.webService.implementations.emoticon.EmoticonManager;
import cn.codetector.util.Validator.MD5;
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
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@WebAPIImpl(prefix = "v2")
public class APIImplV2 implements IWebAPIImpl {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Set<String> noAuthExceptions = new HashSet<>();

    @Override
    public void initAPI(@NotNull Router router, @NotNull Vertx sharedVertx, @NotNull JDBCClient dbClient) {
        //Register exceptions
        noAuthExceptions.add("/v2/api/auth");

        router.options("/api/*").handler(ctx -> {
            ctx.response().putHeader("Access-Control-Allow-Origin", "*");
            ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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
        router.post("/api/*").handler(ctx -> {
            ctx.request().setExpectMultipart(true);
            ctx.request().endHandler(aVoid -> {
                ctx.next();
            });
        });
        router.route("/api/*").handler(ctx -> {
            String path = ctx.request().path();
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (noAuthExceptions.contains(path)) {
                ctx.next();
            } else {
                String auth = ctx.request().getHeader("Authorization").replace("Bearer ", "");
                if (UserHash.INSTANCE.isAuthKeyValid(auth)) {
                    ctx.setUser(UserHash.INSTANCE.getUserByAuthKey(auth));
                    ctx.next();
                } else {
                    logger.info(ctx.request().getHeader("Authorization"));
                    ctx.fail(401);
                }
            }
        });
        router.post("/api/auth").handler(ctx -> {
            if (UserManager.INSTANCE.hasUser(ctx.request().getFormAttribute("username"))) {
                User user = UserManager.INSTANCE.getUserByUsername(ctx.request().getFormAttribute("username"));
                if (user.authenticate(ctx.request().getFormAttribute("password"))) {
                    String hash = UserHash.INSTANCE.createWebUser(user);
                    ctx.response().end(new JsonObject().put("token", hash).toString());
                } else {
                    ctx.fail(401);
                }
            } else {
                ctx.fail(401);
            }
        });
        router.post("/api/auth/verify").handler ( ctx ->{
            ctx.response().end(ctx.user().principal().put("emoticon", EmoticonManager.get()).toString());
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
                            conn.result().query("SELECT `eventId`, `eventName`, UNIX_TIMESTAMP(`eventTime`) as `eventTime`, `eventStatus` FROM `dummyevent` WHERE `eventStatus` >= 0 ORDER BY `eventTime` DESC", res -> {
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
        router.post("/api/event/:eventId/checkout").handler(ctx -> {
            Logger logger = LoggerFactory.getLogger("apiv2.handler.event.checkout");
            String eventId = ctx.pathParam("eventId");
            ctx.user().isAuthorised("checkoutEvent", a -> {
                if (a.result()) {
                    dbClient.getConnection(conn -> {
                        if (conn.succeeded()) {
                            conn.result().queryWithParams("SELECT * FROM `dummyevent` WHERE `eventId` = ?", new JsonArray().add(eventId), handler -> {
                                if (handler.succeeded()) {
                                    if (handler.result().getNumRows() == 1) {
                                        if (handler.result().getRows().get(0).getInteger("eventStatus") < 2) {
                                            conn.result().updateWithParams("UPDATE `dummyevent` SET `eventStatus` = -1 WHERE `eventId` = ?", new JsonArray().add(eventId), handler1 -> {
                                                if (handler1.succeeded()) {
                                                    String accessKey = UUID.randomUUID().toString();
                                                    conn.result().updateWithParams("INSERT INTO `dummyofflineauth` (`eventId`, `authkey`) VALUES (?,?) ON DUPLICATE KEY UPDATE `authkey`=VALUES(`authkey`)", new JsonArray().add(eventId).add(accessKey), handler11 -> {
                                                        if (handler11.succeeded()) {
                                                            conn.result().queryWithParams("SELECT `studentID` as `studentId` ,`checkinTime`, NULL as `checkoutTime` FROM `dummycheck` WHERE `event` = ?", new JsonArray().add(eventId), handler2 -> {
                                                                if (handler2.succeeded()) {
                                                                    ctx.response().end(new JsonObject().put("returnKey", accessKey).put("students", handler2.result().getRows()).toString());
                                                                } else {
                                                                    logger.error("Failed to execute sql query", handler2.cause());
                                                                    ctx.fail(500);
                                                                    conn.result().close();
                                                                }
                                                            });
                                                        } else {
                                                            logger.error("Failed to execute sql query - UUID Key Assignment", handler11.cause());
                                                            ctx.fail(500);
                                                            conn.result().close();
                                                        }
                                                    });
                                                } else {
                                                    logger.error("Failed to execute sql query", handler1.cause());
                                                    ctx.fail(500);
                                                    conn.result().close();
                                                }
                                            });
                                        } else {
                                            ctx.fail(400);
                                            logger.warn("Attempted to checkout a completed event. Giving Up");
                                            conn.result().close();
                                        }
                                    } else {
                                        ctx.fail(400);
                                        logger.warn("Multiple or No event match found. Giving up");
                                        conn.result().close();
                                    }
                                } else {
                                    logger.error("Failed to execute sql query", handler.cause());
                                    ctx.fail(500);
                                    conn.result().close();
                                }
                            });
                        } else {
                            logger.error("Failed to obtain db connection", conn.cause());
                            ctx.fail(500);
                        }
                    });
                } else {
                    ctx.fail(401);
                }
            });
        });
        router.post("/api/event/:eventId/return").handler(ctx -> {
            String eventId = ctx.pathParam("eventId");
            String authKey = ctx.request().getFormAttribute("authKey");
            ctx.user().isAuthorised("updateEvent", v -> {
                if (v.result()) {
                    dbClient.getConnection(con -> {
                        if (con.succeeded()) {
                            con.result().queryWithParams("SELECT * FROM dummyofflineauth WHERE `authkey` = ?", new JsonArray().add(authKey), result -> {
                                if (result.succeeded() && result.result().getNumRows() == 1) {
                                    if (result.result().getRows().get(0).getString("eventId").equalsIgnoreCase(eventId)) {
                                        con.result().updateWithParams("UPDATE `dummyevent` SET `eventStatus` = 1 WHERE `eventId` = ?", new JsonArray().add(eventId), res -> {
                                            if (result.succeeded()) {
                                                con.result().updateWithParams("DELETE FROM `dummyofflineauth` WHERE `eventId` = ?", new JsonArray().add(eventId), r -> {
                                                    ctx.response().end();
                                                    con.result().close();
                                                });
                                            } else {
                                                ctx.fail(500);
                                                con.result().close();
                                            }
                                        });
                                    } else {
                                        con.result().close();
                                        ctx.fail(401);
                                    }
                                } else {
                                    con.result().close();
                                    ctx.fail(result.cause());
                                }
                            });
                        } else {
                            ctx.fail(500);
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
                    logger.trace("Add Request TargetEvent:" + eventId + " DATA:" + dataStr);
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
                                    if (stuObj.getValue("checkout") instanceof String) {
                                        outTime = stuObj.getString("checkout");
                                    } else {
                                        outTime = String.valueOf(stuObj.getLong("checkout"));
                                    }
                                    if (stuObj.getValue("checkin") instanceof String) {
                                        inTime = stuObj.getString("checkin");
                                    } else {
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
                                if (eventStatus < 2 && eventStatus >= 0) {
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
