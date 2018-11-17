- 프로그램 설명
  - 안드로이드 카메라를 이용하여 왼손을 촬영하고 촬영 이미지를 이용하여 왼손의 손톱 위치를 찾아내어 네일을 그 위치에 씌워보는 프로그램

- 전처리과정

  - 이미지를 YCrCb 형태로 변환하고 피부색에 해당하는 임계값을 이용하여 피부색만을 검출하고 그레이화 시킨다.
  - 그레이화한 이미지에서 가장 큰 형체를 찾아내고 그 형체의 외곽 포인터를 모두 구한다.
  - 가장 큰 형체의 defect 포인터들을 검출하고, 중지 양끝의 defect 포인터간의 길이를 구한다.
  - 외곽 포인터 사이의 거리가 방금 구한 길이보다 크고, 엄지와 검지사이의 defect 점의 y값보다 큰 외곽포인터들을 구한다.
  - 위과정을 외곽포인터의 시작부터 끝까지와 끝부터 시작까지로 두번 검출하여 두포인터의 중간지점을 손톱으로 판단한다
  <img width = "900" src = "https://github.com/KevinGuen/DetectNail/blob/master/119856543.png"/>
  
- 스크릿샷

    <img width = "200" src = "https://github.com/KevinGuen/DetectNail/blob/master/111424.png"/> 
    <img width = "200" src = "https://github.com/KevinGuen/DetectNail/blob/master/154828.png"/>
    <img width = "200" src = "https://github.com/KevinGuen/DetectNail/blob/master/1529309512705.jpg"/>
    <img width = "200" src = "https://github.com/KevinGuen/DetectNail/blob/master/1532673755777.jpg"/>
    <img width = "200" src = "https://github.com/KevinGuen/DetectNail/blob/master/1542075216241.jpg"/>
 
