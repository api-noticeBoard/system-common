### Apache POI Excel 스타일링 옵션

1. 글꼴(Font)<br>
**셀 내 텍스트의 모양을 정의**
- setFont(Font font): Font 객체를 생성하여 글꼴, 크기, 색상, 굵기, 이탤릭체, 밑줄 등을 설정합니다.
- Font 객체 설정 예시:
- font.setFontName("Calibri")**: 글꼴 이름 설정
- font.setFontHeightInPoints((short) 12): 글꼴 크기 설정 (포인트 단위)
- font.setBold(true): 굵게 설정
- font.setItalic(true): 이탤릭체 설정
- setStrikeout(boolean): 취소선 설정
- font.setUnderline(Font.U_SINGLE): 단일 밑줄 설정
- font.setColor(IndexedColors.BLUE.getIndex()): 글꼴 색상 설정 (예: 파란색)
---
2. 배경색 및 패턴 (Fill)<br>
**셀의 배경색과 채우기 패턴을 설정**
- setFillBackgroundColor(short bg): 셀 배경의 기본색을 설정합니다. (패턴과 함께 사용 시)
- setFillForegroundColor(short fg): 셀 배경의 채우기 색상을 설정합니다.
- setFillPattern(FillPatternType fillPattern): 셀 배경의 채우기 패턴을 설정합니다.
  - FillPatternType.SOLID_FOREGROUND: 단색으로 채우기 (가장 흔하게 사용)
  - FillPatternType.PATTERN_GRAY_125: 회색 12.5% 패턴
  - FillPatternType.PATTERN_GRAY_25: 회색 25% 패턴
  - FillPatternType.PATTERN_GRAY_50: 회색 50% 패턴
  - FillPatternType.PATTERN_GRAY_75: 회색 75% 패턴
  - FillPatternType.BRICKS: 벽돌 패턴
  - FillPatternType.DIAGONAL_BRICK: 대각선 벽돌 패턴
  - FillPatternType.DOTTED: 점선 패턴
  - FillPatternType.DIAGONAL_UP_RIGHT: 우상향 대각선 패턴
  - FillPatternType.SPARSE_DOTS: 듬성듬성한 점선 패턴
  - FillPatternType.SQUARES: 정사각형 패턴
  - FillPatternType.THICK_BACKWARD_DIAGONAL: 두꺼운 후방 대각선 패턴
  - FillPatternType.THICK_FORWARD_DIAGONAL: 두꺼운 전방 대각선 패턴
  - FillPatternType.THICK_HORIZONTAL_STRIPES: 두꺼운 수평 줄무늬 패턴
  - FillPatternType.THICK_PARA_DIAGONAL: 두꺼운 평행 대각선 패턴
  - FillPatternType.THICK_VERTICAL_STRIPES: 두꺼운 수직 줄무늬 패
---
3. 정렬 (Alignment)<br>
**셀 내 텍스트의 가로 및 세로 정렬을 설정**
- setAlignment(HorizontalAlignment alignment): 텍스트의 가로 정렬을 설정합니다.
  - HorizontalAlignment.LEFT
  - HorizontalAlignment.CENTER
  - HorizontalAlignment.RIGHT
  - HorizontalAlignment.GENERAL (기본값: 숫자는 오른쪽, 텍스트는 왼쪽)
- setVerticalAlignment(VerticalAlignment alignment): 텍스트의 세로 정렬을 설정합니다.
  - VerticalAlignment.TOP
  - VerticalAlignment.CENTER
  - VerticalAlignment.BOTTOM
- setWrapText(boolean)	셀 안에 텍스트가 줄바꿈되도록 설정
- setIndention(short)	텍스트 들여쓰기 설정
---
4. 테두리 (Border)<br>
**셀의 상하좌우 테두리 모양과 색상을 설정**
- setBorderLeft(BorderStyle border): 왼쪽 테두리 스타일 설정
- setBorderRight(BorderStyle border): 오른쪽 테두리 스타일 설정
- setBorderTop(BorderStyle border): 위쪽 테두리 스타일 설정
- setBorderBottom(BorderStyle border): 아래쪽 테두리 스타일 설정
  - BorderStyle 예시: <br>BorderStyle.THIN (얇은 선), <br>BorderStyle.THICK (두꺼운 선), <br>BorderStyle.DOUBLE (이중 선), <br>BorderStyle.DASHED (점선)
- setLeftBorderColor(short color): 왼쪽 테두리 색상 설정
- setRightBorderColor(short color): 오른쪽 테두리 색상 설정
- setTopBorderColor(short color): 위쪽 테두리 색상 설정
- setBottomBorderColor(short color): 아래쪽 테두리 색상 설정
---
5. 데이터 형식 (DataFormat)<br>
**셀에 표시될 데이터의 형식을 지정**
- setDataFormat(short formatIndex): DataFormat 객체를 사용하여 숫자, 날짜, 통화, 백분율 등 다양한 형식으로 지정<br>
  - 예시: workbook.createDataFormat().getFormat("0.00") (소수점 둘째 자리까지 표시), <br>
  workbook.createDataFormat().getFormat("yyyy-MM-dd") (날짜 형식)
---
6. 감싸기 및 회전 (WrapText, Rotation)
- setWrapText(boolean wrap): 텍스트가 셀 너비를 초과할 때 자동으로 줄바꿈하여 표시할지 여부를 설정
- setRotation(short rotation): 셀 내 텍스트의 회전 각도를 설정합니다. (예: 텍스트를 세로로 표시)
---
7. 들여쓰기 (Indentation)
- setIndentation(short indentation): 셀 내용의 들여쓰기 정도를 설정합니다.