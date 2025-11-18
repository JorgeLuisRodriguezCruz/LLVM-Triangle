@last_char = global i32 0

; Format strings
@.str.int = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1
@.str.int.scanf = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str.char = private unnamed_addr constant [3 x i8] c"%c\00", align 1
@.str.bool = private unnamed_addr constant [4 x i8] c"%s\0A\00", align 1
@.str.true = private unnamed_addr constant [5 x i8] c"true\00", align 1
@.str.false = private unnamed_addr constant [6 x i8] c"false\00", align 1

; Standard Library Functions
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
declare i32 @putchar(i32)
declare i32 @getchar()

define i1 @eol() {
entry:
  %last = load i32, i32* @last_char
  %cmp = icmp eq i32 %last, 10  ; '\n'
  br i1 %cmp, label %true, label %check_eof
  check_eof:
  %cmp2 = icmp eq i32 %last, -1  ; EOF
  br i1 %cmp2, label %true, label %false
  true:
  ret i1 true
  false:
  ret i1 false
}

define void @geteol() {
entry:
  %ch = call i32 @getchar()
  store i32 %ch, i32* @last_char
  ret void
}

define void @puteol() {
entry:
  %result = call i32 @putchar(i32 10)  ; '\n'
  ret void
}

define void @getint(i32* %ptr) {
entry:
  %result = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.int.scanf, i32 0, i32 0), i32* %ptr)
  ret void
}

define i32 @min(i32 %x,i32 %y) {
entry:
  %temp0 = load i32, i32* %x
  %temp1 = load i32, i32* %y
  %temp2 = icmp slt i32 %temp0, %temp1
  br i1 %temp2, label %label0, label %label1
  label0:
  %temp4 = load i32, i32* %x
  br label %label2
  label1:
  %temp5 = load i32, i32* %y
  br label %label2
  label2:
  %temp3 = phi i32 [ %temp4, %label0 ], [ %temp5, %label1 ]
  ret i32 %temp3
}

define i32 @main() {
entry:
  %a = alloca i32
  %b = alloca i32
  call void @getint(%a)
  call void @getint(%b)
  %temp6 = load i32, i32* %a
  %temp7 = load i32, i32* %b
  %temp8 = call i32 @min(%temp6,%temp7)
  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.int, i32 0, i32 0), i32 %temp8)
  call void @puteol()
  ret i32 0
}
