package com.yu.dubbo.core.handle;

import com.yu.dubbo.utils.SpringContextHolder;
import com.yu.dubbo.core.codec.CodecUtil;
import com.yu.dubbo.core.protocol.RequestDomain;
import com.yu.dubbo.core.protocol.ResponseDomain;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 5:14
 */
public class AppServer extends HttpServlet {
    private static Logger log = LoggerFactory.getLogger(AppServer.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getOutputStream().write("not support get method".getBytes());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        call(req, resp);
    }

    private void call(HttpServletRequest req, HttpServletResponse resp) {

        try {
            OutputStream os = resp.getOutputStream();
            byte[] requestData = parseRequestData(req);
            long start = System.currentTimeMillis();
            Object result = null;

            RequestDomain appRequestDomain = null;
            ResponseDomain appResponseDomain = new ResponseDomain();

            try {
                appRequestDomain = CodecUtil.decodeRequest(requestData);

                if (appRequestDomain.getClassName() == null) {
                    throw new RuntimeException("[app-server] class does not exist");
                }
                String className = appRequestDomain.getClassName();
                String methodName = appRequestDomain.getMethodName();
                String[] paramTypeNames = appRequestDomain.getParamTypeNames();
                Object[] paramInputs = appRequestDomain.getParamInputs();

                Class<?> cls = Class.forName(className);
                Class<?>[] inputTypes = null;
                if (paramTypeNames != null) {
                    inputTypes = new Class<?>[paramTypeNames.length];
                    for (int i = 0; i < paramTypeNames.length; i++) {
                        inputTypes[i] = Class.forName(paramTypeNames[i]);
                    }
                }

                Method method = null;
                if (inputTypes != null && inputTypes.length > 0) {
                    method = ReflectionUtils.findMethod(cls, methodName, inputTypes);
                } else {
                    method = ReflectionUtils.findMethod(cls, methodName);
                }
                if (method == null) {
                    throw new RuntimeException("[app-server] method does not exist : " + methodName);
                }
                Object targetObject = SpringContextHolder.getBean(cls);
                if (targetObject == null) {
                    throw new RuntimeException("[app-server] no interface implementation class found" + cls.getName());
                }
                if (paramInputs != null && paramInputs.length > 0) {
                    result = ReflectionUtils.invokeMethod(method, targetObject, paramInputs);
                } else {
                    result = ReflectionUtils.invokeMethod(method, targetObject);
                }
                appResponseDomain.setCode(0);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String msg = e.getMessage();

                appResponseDomain.setCode(501);

                if (msg != null && !msg.matches("\\s*")) {
                    appResponseDomain.setMessage(msg);
                } else {
                    appResponseDomain.setMessage("[app-server] service exception");
                }
            } finally {
                long end = System.currentTimeMillis();
                appResponseDomain.setCostTime((int) (end - start));
                appResponseDomain.setResult(result);

                if (os != null) {
                    try {
                        byte[] out = CodecUtil.encodeResponse(appResponseDomain);
                        os.write(out);
                    } catch (Exception e2) {
                        log.error(e2.getMessage(), e2);
                    }
                    try {
                        os.flush();
                        os.close();
                    } catch (Exception e2) {
                        log.error(e2.getMessage(), e2);
                    }
                }

                if (appRequestDomain != null) {
                    log.info("[app-server] execute done, from client ip: {}, class: {}, method: {}, cost: {}ms ", req.getRemoteAddr(), appRequestDomain.getClassName(), appRequestDomain.getMethodName(), (end - start));
                } else {
                    log.info("[app-server] from client ip: {}, cost: {}ms ", req.getRemoteAddr(), (end - start));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static byte[] parseRequestData(HttpServletRequest request) throws Exception {

        request.setCharacterEncoding("UTF-8");

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload servletFileUpload = new ServletFileUpload(factory);
        // 最大5M
        servletFileUpload.setSizeMax(5 * 1024 * 1024);

        List<FileItem> fileItems = (List<FileItem>) servletFileUpload.parseRequest(request);

        // 依次处理每个上传的文件
        byte[] data = null;
        Iterator<FileItem> it = fileItems.iterator();
        while (it.hasNext()) {

            FileItem item = it.next();

            if (!item.isFormField()) {
                java.io.InputStream is = item.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                int len = -1;
                byte[] buf = new byte[10240];
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                is.close();
                bos.flush();
                bos.close();
                data = bos.toByteArray();
            }
        }
        return data;
    }
}
