package com.portolio.common.system.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * [Utility Class]
 * 파일 업로드, 삭제, 파싱 등 파일 I/O 관련 편의 기능을 제공하는 유틸리티 클래스.
 * 이 클래스는 인스턴스화할 필요가 없도록 설계.
 */
public final class FileUtils {

    /**
     * 유틸리티 클래스는 인스턴스화할 필요가 없으므로 private 생성자로 막음.
     */
    private FileUtils() {}

    /**
     * MultipartFile(이미지, 문서 등 모든 종류의 파일)을 지정된 경로에 저장.
     * 파일 이름은 서버에서 중복되지 않도록 UUID를 사용하여 고유한 이름으로 새로 생성.
     * 원본 파일의 확장자는 유지.
     *
     * @param multipartFile 컨트롤러에서 받은 업로드 파일 객체
     * @param uploadPath    파일을 저장할 서버의 디렉터리 경로 (예: "/path/to/uploads/")
     * @return 데이터베이스 등에 저장할, 서버에 실제로 저장된 파일의 새로운 이름 (확장자 포함)
     * @throws IOException 디렉터리 생성이나 파일 쓰기 실패 시
     */
    public static String saveFile(MultipartFile multipartFile, String uploadPath) throws IOException {
        // 파일이 비어있거나 존재하지 않으면 null을 반환하여 아무 작업도 하지 않음
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        // 파일을 저장할 디렉터리 객체를 생성.
        File uploadDir = new File(uploadPath);
        // 만약 디렉터리가 존재하지 않으면, 부모 디렉터리까지 포함하여 모두 생성. (mkdirs)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 1. 원본 파일 이름에서 확장자를 추출.
        String originalFileName = multipartFile.getOriginalFilename();
        String extension = "";
        // 파일 이름에 '.'이 포함된 경우에만 확장자를 추출.
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 2. UUID를 사용하여 고유한 파일 이름을 생성하고, 추출한 확장자를 붙임.
        // 예: "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6.jpg"
        String newFileName = UUID.randomUUID().toString() + extension;

        // 3. 최종 저장 경로(Path 객체)를 생성.
        Path destinationPath = Paths.get(uploadPath, newFileName);

        // 4. MultipartFile의 transferTo() 메서드를 사용하여 임시 저장된 업로드 파일을 최종 목적지로 옮김.
        multipartFile.transferTo(destinationPath);

        // 5. 서버에 저장된 새로운 파일 이름을 반환.
        return newFileName;
    }

    /**
     * 지정된 경로의 파일을 삭제.
     * 파일이 존재하지 않더라도 예외를 발생시키지 않음.
     * @param filePath 삭제할 파일의 전체 경로 (예: "/path/to/uploads/a1b2c3d4.jpg")
     */
    public static void deleteFile(String filePath) {
        try {
            // Java NIO의 Files.deleteIfExists()를 사용하여 파일을 안전하게 삭제.
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // 파일 삭제 실패는 치명적인 오류가 아닐 수 있으므로, 에러 로그만 남기고 계속 진행.
            // 필요에 따라 로깅 프레임워크(Slf4j)를 사용하여 log.error()로 변경.
            System.err.println("파일 삭제 실패: " + filePath + ", 원인: " + e.getMessage());
        }
    }
}
