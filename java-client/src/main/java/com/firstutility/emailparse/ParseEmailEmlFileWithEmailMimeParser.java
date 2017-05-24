package com.firstutility.emailparse;

import static java.util.stream.Collectors.toMap;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseEmailEmlFileWithEmailMimeParser {

    private String getTxtPart(final Entity part) throws Exception {
        TextBody tb = (TextBody) part.getBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tb.writeTo(baos);
        return new String(baos.toByteArray());
    }

    private void parseBodyParts(final Multipart multipart, final StringBuffer txtBody,
            final StringBuffer htmlBody, final List<Entity> attachments) throws Exception {
        for (Entity part : multipart.getBodyParts()) {
            if (part.getMimeType().equals("text/plain")) {
                String txt = getTxtPart(part);
                txtBody.append(txt);
            } else if (part.getMimeType().equals("text/html")) {
                String html = getTxtPart(part);
                htmlBody.append(html);
            } else if (part.getDispositionType() != null && !part.getDispositionType().equals("")) {
                //If DispositionType is null or empty, it means that it's multipart, not attached file
                attachments.add(part);
            }

            if (part.isMultipart()) {
                parseBodyParts((Multipart) part.getBody(), txtBody, htmlBody, attachments);
            }
        }
    }

    public String parseMessage(final String fileName) throws Exception {

        final FileInputStream fis = new FileInputStream(fileName);

        final Message mimeMsg = new DefaultMessageBuilder().parseMessage(fis);

        final StringBuffer txtBody = new StringBuffer();
        final StringBuffer htmlBody = new StringBuffer();
        final List<Entity> attachments = new ArrayList();

        //System.out.println("To: " + mimeMsg.getTo().toString());
        //System.out.println("From: " + mimeMsg.getFrom().toString());
        //System.out.println("Subject: " + mimeMsg.getSubject());

        Field priorityFld = mimeMsg.getHeader().getField("X-Priority");
        if (priorityFld != null) {
            System.out.println("Priority: " + priorityFld.getBody());
        }

        //System.out.println("Multipart: " + mimeMsg.isMultipart());

        if (mimeMsg.isMultipart()) {
            Multipart multipart = (Multipart) mimeMsg.getBody();
            parseBodyParts(multipart, txtBody, htmlBody, attachments);
        } else {
            //If it's single part message, just get text body
            String text = getTxtPart(mimeMsg);
            txtBody.append(text);
        }

        //System.out.println("Text body: " + txtBody.toString());
        //System.out.println("Html body: " + htmlBody.toString());

        for (Entity attach : attachments) {
            String attName = attach.getFilename();

            try(FileOutputStream fos = new FileOutputStream(attName)) {
                System.out.println("output attachment");
                BinaryBody bb = (BinaryBody) attach.getBody();
                bb.writeTo(fos);
            }
        }

        return htmlBody.toString();
    }

    private String getColumnContentAtPosition(final Element row, int pos) {
        final Element column = row.select("td:eq(" + pos+ ")").first();
        return (column != null) ? column.text() : null;
    }

    private String formatHeader(final String value) {
        return (value != null) ? value.toLowerCase().replaceAll(" ","_").replaceAll("/","") : "";
    }

    public Map parseHtmlBody(final String htmlBody) {

        final Map<String, String> parsedValues = new HashMap<>();

        final Document doc = Jsoup.parse(htmlBody);

        final Element cliTable = doc.select("table").first();
        final Element cliRow = cliTable.select("tr").first();
        final String cli = cliRow.select("td").last().text();

        //System.out.println("cli : " + cli);

        parsedValues.put("cli", cli);

        final Element dataTable = doc.select("table").last();
        final Element headerRow = dataTable.select("tr").first();
        final Element dataRow = dataTable.select("tr").last();

        Map<String, String> data = dataRow
                .select("td")
                .stream()
                .collect(toMap(e -> formatHeader(getColumnContentAtPosition(headerRow, e.elementSiblingIndex())), e -> e.text()));

        parsedValues.putAll(data);

        return parsedValues;
    }

    public static void main(String[] args) throws Exception {
        //final String eml = "/Users/slamplugh/Desktop/0.3adexgvtjzy.eml";
        final String eml = "/Users/slamplugh/Desktop/0.34c7t478ft0.eml";

        final ParseEmailEmlFileWithEmailMimeParser parser =
                new ParseEmailEmlFileWithEmailMimeParser();

        final String html = parser.parseMessage(eml);

        final Map<String, String> values = parser.parseHtmlBody(html);

        System.out.println("values : " + values);

    }
}
